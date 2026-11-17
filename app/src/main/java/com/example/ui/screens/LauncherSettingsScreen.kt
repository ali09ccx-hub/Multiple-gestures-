package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.GestureUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldActive
import kotlin.math.roundToInt

@Composable
fun LauncherSettingsScreen(
    state: GestureUiState,
    onSetMasterEnabled: (Boolean) -> Unit,
    onSetFloatingOrb: (Boolean) -> Unit,
    onSetEdgeZones: (Boolean) -> Unit,
    onSetEdgeWidth: (Float) -> Unit,
    onSetOrbOpacity: (Float) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetMultiTapWindow: (Int) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 650.dp)
    ) {
        item {
            Text(
                text = "التشغيل فوق اللانشر المخصص",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "يعمل التطبيق فوق Nova وLawnchair وNiagara وأي لانشر آخر عبر خدمة إمكانية الوصول والأزرار الجانبية العائمة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Step 1: Accessibility Service
        item {
            PermissionStepCard(
                stepTitle = "1. تفعيل خدمة إمكانية الوصول (أساسي)",
                description = "يسمح بعرض الزر العائم وحواف الإيماءات فوق أي لانشر وتنفيذ قفل الشاشة ولقطة الشاشة.",
                icon = Icons.Rounded.AccessibilityNew,
                isGranted = state.isAccessibilityEnabled,
                grantedText = "الخدمة مفعّلة وتعمل الآن",
                actionLabel = if (state.isAccessibilityEnabled) "إدارة الخدمة" else "تفعيل من الإعدادات",
                testTag = "btn_accessibility_settings",
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }
            )
        }

        // Step 2: Overlay Permission (optional extra compatibility)
        item {
            PermissionStepCard(
                stepTitle = "2. إذن الظهور فوق التطبيقات",
                description = "إذن إضافي يضمن استقرار الطبقة العائمة على بعض الرومات مثل MIUI وHyperOS وColorOS.",
                icon = Icons.Rounded.Layers,
                isGranted = state.canDrawOverlays,
                grantedText = "الإذن ممنوح",
                actionLabel = if (state.canDrawOverlays) "تم المنح" else "منح الإذن",
                testTag = "btn_overlay_settings",
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                }
            )
        }

        // Step 3: Battery Optimization
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "3. استثناء البطارية في الخلفية",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "يمنع النظام من إيقاف خدمة الإيماءات عند استخدام اللانشر المخصص لفترات طويلة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.testTag("btn_battery_settings")
                    ) {
                        Text("فتح")
                    }
                }
            }
        }

        // Controls Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تخصيص اللوحة والحواف",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingSwitchRow(
                        title = "تشغيل محرك الإيماءات العام",
                        subtitle = "تفعيل أو إيقاف جميع المشغلات العائمة مؤقتاً",
                        icon = Icons.Rounded.CheckCircle,
                        checked = state.settings.masterEnabled,
                        onCheckedChange = onSetMasterEnabled
                    )

                    SettingSwitchRow(
                        title = "الزر العائم السريع (Floating Orb)",
                        subtitle = "زر متحرك يفتح لوحة اللمس متعددة الأصابع فوق أي تطبيق أو لانشر",
                        icon = Icons.Rounded.Layers,
                        checked = state.settings.floatingOrbEnabled,
                        onCheckedChange = onSetFloatingOrb
                    )

                    if (state.settings.floatingOrbEnabled) {
                        Column {
                            Text(
                                text = "شفافية الزر العائم: ${(state.settings.orbOpacity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = state.settings.orbOpacity,
                                onValueChange = onSetOrbOpacity,
                                valueRange = 0.25f..1.0f
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "حواف الشاشة الجانبية",
                        subtitle = "تنفيذ إيماءات السحب والضغط المطول من حافة اليمين واليسار",
                        icon = Icons.Rounded.Layers,
                        checked = state.settings.edgeZonesEnabled,
                        onCheckedChange = onSetEdgeZones
                    )

                    if (state.settings.edgeZonesEnabled) {
                        Column {
                            Text(
                                text = "سماكة منطقة الحافة: ${state.settings.edgeWidthDp.roundToInt()} dp",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = state.settings.edgeWidthDp,
                                onValueChange = onSetEdgeWidth,
                                valueRange = 12f..34f
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "الاهتزاز التفاعلي (Haptic)",
                        subtitle = "نبضة خفيفة عند التعرف على الإيماءة",
                        icon = Icons.Rounded.Vibration,
                        checked = state.settings.hapticFeedback,
                        onCheckedChange = onSetHaptic
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سرعة الضغط المزدوج (3×2): ${state.settings.multiTapWindowMs} ms",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Slider(
                            value = state.settings.multiTapWindowMs.toFloat(),
                            onValueChange = { onSetMultiTapWindow(it.roundToInt()) },
                            valueRange = 220f..650f
                        )
                    }

                    SettingSwitchRow(
                        title = "الوضع الليلي الداكن",
                        subtitle = "مظهر أوبسيديان داكن عالي التباين",
                        icon = Icons.Rounded.DarkMode,
                        checked = state.settings.darkTheme,
                        onCheckedChange = onSetDarkTheme
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_reset_defaults")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استعادة الإعدادات الافتراضية لجميع الـ 24 إيماءة")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("استعادة الإعدادات الافتراضية؟") },
            text = { Text("سيتم إعادة ضبط اختصارات وتطبيقات جميع الـ 24 إيماءة إلى الوضع الافتراضي.") },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    onResetDefaults()
                }) {
                    Text("استعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun PermissionStepCard(
    stepTitle: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    grantedText: String,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isGranted) EmeraldActive.copy(alpha = 0.65f) else AmberWarning.copy(alpha = 0.65f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldActive else AmberWarning,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stepTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (isGranted) EmeraldActive else AmberWarning,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGranted) grantedText else "غير مفعّل بعد",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isGranted) EmeraldActive else AmberWarning
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
