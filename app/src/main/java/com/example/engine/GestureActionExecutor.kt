package com.example.engine

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.widget.Toast
import com.example.data.ActionType
import com.example.data.GestureEntity
import com.example.service.GestureAccessibilityService

class GestureActionExecutor(private val context: Context) {
    private var isTorchOn = false

    fun execute(gesture: GestureEntity, hapticEnabled: Boolean): ExecutionResult {
        if (hapticEnabled) {
            triggerHaptic()
        }

        val action = gesture.actionTypeEnum()
        return when (action) {
            ActionType.OPEN_APP -> launchAppOrFallback(gesture.targetPackage, gesture.targetUrl, gesture.targetLabel)
            ActionType.OPEN_URL -> openUrl(gesture.targetUrl)
            ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ActionType.MEDIA_PLAY_PAUSE -> dispatchMediaPlayPause()
            ActionType.OPEN_GESTURE_PAD -> {
                val service = GestureAccessibilityService.instance
                if (service != null) {
                    service.showGesturePadOverlay()
                    ExecutionResult.Success("تم فتح لوحة الإيماءات")
                } else {
                    ExecutionResult.NeedsAccessibility("فعّل خدمة إمكانية الوصول لتشغيل اللوحة فوق اللانشر")
                }
            }
            ActionType.SYSTEM_SCREENSHOT,
            ActionType.SYSTEM_LOCK_SCREEN,
            ActionType.SYSTEM_NOTIFICATIONS,
            ActionType.SYSTEM_QUICK_SETTINGS,
            ActionType.SYSTEM_RECENTS,
            ActionType.SYSTEM_SPLIT_SCREEN,
            ActionType.SYSTEM_HOME,
            ActionType.SYSTEM_BACK,
            ActionType.SYSTEM_POWER_DIALOG -> {
                val service = GestureAccessibilityService.instance
                if (service != null) {
                    val ok = service.performSystemAction(action)
                    if (ok) ExecutionResult.Success("تم تنفيذ: ${action.titleAr}")
                    else ExecutionResult.Error("هذا الاختصار غير مدعوم على إصدار النظام الحالي")
                } else {
                    ExecutionResult.NeedsAccessibility("فعّل خدمة إمكانية الوصول لتنفيذ: ${action.titleAr}")
                }
            }
        }
    }

    private fun launchAppOrFallback(packageName: String?, fallbackUrl: String?, label: String): ExecutionResult {
        if (!packageName.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                return runCatching {
                    context.startActivity(launchIntent)
                    ExecutionResult.Success("تم فتح $label")
                }.getOrElse {
                    ExecutionResult.Error("تعذّر فتح $label")
                }
            }
        }

        if (!fallbackUrl.isNullOrBlank()) {
            val opened = openUrl(fallbackUrl)
            if (opened is ExecutionResult.Success) {
                return ExecutionResult.Success("تم فتح $label عبر المتصفح (التطبيق غير مثبت)")
            }
        }

        Toast.makeText(context, "تطبيق $label غير مثبت على الجهاز حالياً", Toast.LENGTH_SHORT).show()
        return ExecutionResult.Error("تطبيق $label غير مثبت على الجهاز")
    }

    private fun openUrl(url: String?): ExecutionResult {
        if (url.isNullOrBlank()) return ExecutionResult.Error("الرابط غير محدد")
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            ExecutionResult.Success("تم فتح الرابط")
        }.getOrElse {
            ExecutionResult.Error("لا يوجد متصفح متاح لفتح الرابط")
        }
    }

    private fun toggleFlashlight(): ExecutionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ExecutionResult.Error("الكشاف غير متاح")

        return runCatching {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ExecutionResult.Error("لا يوجد فلاش على هذا الجهاز")

            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            ExecutionResult.Success(if (isTorchOn) "تم تشغيل الكشاف" else "تم إطفاء الكشاف")
        }.getOrElse {
            ExecutionResult.Error("تعذّر التحكم بالكشاف")
        }
    }

    private fun dispatchMediaPlayPause(): ExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ExecutionResult.Error("التحكم بالوسائط غير متاح")
        val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
        return ExecutionResult.Success("تم إرسال أمر تشغيل/إيقاف الوسائط")
    }

    private fun triggerHaptic() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(32L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(32L)
                }
            }
        }
    }
}

sealed interface ExecutionResult {
    val message: String
    data class Success(override val message: String) : ExecutionResult
    data class NeedsAccessibility(override val message: String) : ExecutionResult
    data class Error(override val message: String) : ExecutionResult
}
