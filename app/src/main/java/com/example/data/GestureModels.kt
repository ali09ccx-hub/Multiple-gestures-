package com.example.data

import android.graphics.drawable.Drawable

enum class GestureCategory(val titleAr: String) {
    ALL("الكل"),
    MULTI_TOUCH("أصابع متعددة"),
    SWIPE("سحب ذكي"),
    EDGE_PINCH("الحواف والقرص"),
    DRAW("رسم الحروف")
}

enum class GestureTriggerType(
    val sortOrder: Int,
    val titleAr: String,
    val subtitleAr: String,
    val badgeLabel: String,
    val category: GestureCategory
) {
    THREE_FINGER_DOUBLE_TAP(
        1,
        "ضغطتين بـ 3 أصابع",
        "ضع ثلاثة أصابع واضغط مرتين متتاليتين بسرعة",
        "3×2",
        GestureCategory.MULTI_TOUCH
    ),
    THREE_FINGER_TAP(
        2,
        "ضغطة واحدة بـ 3 أصابع",
        "لمسة واحدة بثلاثة أصابع على اللوحة أو الشاشة",
        "3×1",
        GestureCategory.MULTI_TOUCH
    ),
    THREE_FINGER_HOLD(
        3,
        "ضغطة مطولة بـ 3 أصابع",
        "ثبّت ثلاثة أصابع لمدة نصف ثانية",
        "3◉",
        GestureCategory.MULTI_TOUCH
    ),
    TWO_FINGER_DOUBLE_TAP(
        4,
        "ضغطتين بإصبعين",
        "نقر سريع مزدوج بإصبعين معاً",
        "2×2",
        GestureCategory.MULTI_TOUCH
    ),
    TWO_FINGER_HOLD(
        5,
        "ضغطة مطولة بإصبعين",
        "ثبّت إصبعين معاً لتنفيذ الإجراء",
        "2◉",
        GestureCategory.MULTI_TOUCH
    ),
    FOUR_FINGER_TAP(
        6,
        "ضغطة بـ 4 أصابع",
        "لمسة سريعة بأربعة أصابع في آن واحد",
        "4×1",
        GestureCategory.MULTI_TOUCH
    ),
    FOUR_FINGER_DOUBLE_TAP(
        7,
        "ضغطتين بـ 4 أصابع",
        "نقر مزدوج بأربعة أصابع معاً",
        "4×2",
        GestureCategory.MULTI_TOUCH
    ),
    FIVE_FINGER_TAP(
        8,
        "لمسة راحة اليد (5 أصابع)",
        "اضغط بخمسة أصابع على الشاشة دفعة واحدة",
        "5×1",
        GestureCategory.MULTI_TOUCH
    ),

    THREE_FINGER_SWIPE_UP(
        9,
        "سحب للأعلى بـ 3 أصابع",
        "اسحب للأعلى بثلاثة أصابع بالتوازي",
        "3↑",
        GestureCategory.SWIPE
    ),
    THREE_FINGER_SWIPE_DOWN(
        10,
        "سحب للأسفل بـ 3 أصابع",
        "اسحب للأسفل بثلاثة أصابع بالتوازي",
        "3↓",
        GestureCategory.SWIPE
    ),
    THREE_FINGER_SWIPE_RIGHT(
        11,
        "سحب لليمين بـ 3 أصابع",
        "اسحب باتجاه اليمين بثلاثة أصابع",
        "3→",
        GestureCategory.SWIPE
    ),
    THREE_FINGER_SWIPE_LEFT(
        12,
        "سحب لليسار بـ 3 أصابع",
        "اسحب باتجاه اليسار بثلاثة أصابع",
        "3←",
        GestureCategory.SWIPE
    ),
    TWO_FINGER_SWIPE_UP(
        13,
        "سحب للأعلى بإصبعين",
        "اسحب للأعلى بإصبعين معاً",
        "2↑",
        GestureCategory.SWIPE
    ),
    TWO_FINGER_SWIPE_DOWN(
        14,
        "سحب للأسفل بإصبعين",
        "اسحب للأسفل بإصبعين معاً",
        "2↓",
        GestureCategory.SWIPE
    ),

    EDGE_LEFT_SWIPE(
        15,
        "سحب من الحافة اليسرى",
        "اسحب للداخل من الشريط الجانبي الأيسر فوق اللانشر",
        "⇤",
        GestureCategory.EDGE_PINCH
    ),
    EDGE_RIGHT_SWIPE(
        16,
        "سحب من الحافة اليمنى",
        "اسحب للداخل من الشريط الجانبي الأيمن فوق اللانشر",
        "⇥",
        GestureCategory.EDGE_PINCH
    ),
    EDGE_LEFT_HOLD(
        17,
        "لمس مطول بالحافة اليسرى",
        "اسحب من الحافة اليسرى وثبّت للحظة",
        "⇤◉",
        GestureCategory.EDGE_PINCH
    ),
    EDGE_RIGHT_HOLD(
        18,
        "لمس مطول بالحافة اليمنى",
        "اسحب من الحافة اليمنى وثبّت للحظة",
        "◉⇥",
        GestureCategory.EDGE_PINCH
    ),
    TWO_FINGER_PINCH_IN(
        19,
        "ضم بإصبعين للداخل",
        "قرّب إصبعين باتجاه بعضهما (Pinch In)",
        "≻≺",
        GestureCategory.EDGE_PINCH
    ),
    TWO_FINGER_PINCH_OUT(
        20,
        "توسيع بإصبعين للخارج",
        "باعد بين إصبعين للخارج (Pinch Out)",
        "≺≻",
        GestureCategory.EDGE_PINCH
    ),

    DRAW_CIRCLE(
        21,
        "رسم دائرة (O)",
        "ارسم دائرة مغلقة بإصبع واحد على اللوحة",
        "O",
        GestureCategory.DRAW
    ),
    DRAW_V(
        22,
        "رسم حرف (V)",
        "ارسم شكل V بإصبع واحد من الأعلى للأسفل ثم للأعلى",
        "V",
        GestureCategory.DRAW
    ),
    DRAW_L(
        23,
        "رسم حرف (L)",
        "ارسم زاوية قائمة L للأسفل ثم لليمين",
        "L",
        GestureCategory.DRAW
    ),
    DRAW_Z(
        24,
        "رسم حرف (Z)",
        "ارسم خطاً متعرجاً بشكل Z",
        "Z",
        GestureCategory.DRAW
    )
}

enum class ActionType(
    val titleAr: String,
    val descriptionAr: String,
    val requiresAccessibility: Boolean
) {
    OPEN_APP("فتح تطبيق", "تشغيل تطبيق مثبت على الهاتف", false),
    OPEN_URL("فتح رابط ويب", "فتح رابط مخصص في المتصفح", false),
    SYSTEM_SCREENSHOT("لقطة شاشة", "التقاط صورة فورية للشاشة", true),
    SYSTEM_LOCK_SCREEN("قفل الشاشة", "إطفاء وقفل شاشة الهاتف فوراً", true),
    SYSTEM_NOTIFICATIONS("لوحة الإشعارات", "إنزال شريط الإشعارات", true),
    SYSTEM_QUICK_SETTINGS("الإعدادات السريعة", "فتح لوحة الاختصارات السريعة", true),
    SYSTEM_RECENTS("التطبيقات الحديثة", "عرض التطبيقات المفتوحة مؤخراً", true),
    SYSTEM_SPLIT_SCREEN("تقسيم الشاشة", "تفعيل وضع الشاشة المنقسمة", true),
    SYSTEM_HOME("الشاشة الرئيسية", "الرجوع إلى اللانشر الرئيسي", true),
    SYSTEM_BACK("زر الرجوع", "تنفيذ أمر العودة للخلف", true),
    SYSTEM_POWER_DIALOG("قائمة الطاقة", "إظهار قائمة إيقاف التشغيل", true),
    TOGGLE_FLASHLIGHT("تشغيل / إيقاف الكشاف", "التحكم السريع بفلاش الكاميرا", false),
    MEDIA_PLAY_PAUSE("تشغيل / إيقاف الوسائط", "التحكم بتشغيل الصوت والفيديو", false),
    OPEN_GESTURE_PAD("فتح لوحة الإيماءات", "إظهار لوحة اللمس متعددة الأصابع فوق اللانشر", true)
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isInstalled: Boolean = true
)

data class AppPreset(
    val label: String,
    val packageName: String,
    val fallbackUrl: String
)

object PopularAppPresets {
    val presets = listOf(
        AppPreset("YouTube", "com.google.android.youtube", "https://m.youtube.com"),
        AppPreset("Claude", "com.anthropic.claude", "https://claude.ai"),
        AppPreset("WhatsApp", "com.whatsapp", "https://web.whatsapp.com"),
        AppPreset("Telegram", "org.telegram.messenger", "https://web.telegram.org"),
        AppPreset("Chrome", "com.android.chrome", "https://www.google.com"),
        AppPreset("Spotify", "com.spotify.music", "https://open.spotify.com"),
        AppPreset("X / Twitter", "com.twitter.android", "https://x.com"),
        AppPreset("Instagram", "com.instagram.android", "https://instagram.com")
    )
}
