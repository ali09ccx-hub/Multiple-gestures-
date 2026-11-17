# تطبيق إيماءات (Gestures Android App)

تطبيق أندرويد لإيماءات الشاشة متعددة الأصابع والاختصارات السريعة، يعمل فوق أي لانشر مخصص عبر خدمة إمكانية الوصول (AccessibilityService)، الزر العائم، وحواف الشاشة.

## تحويل المشروع إلى APK عبر GitHub Actions

تم تضمين ملف سير العمل جاهزاً في:
`.github/workflows/build-apk.yml`

### خطوات استخراج ملف APK من GitHub:
1. ارفع جميع ملفات المشروع (بما فيها مجلد `.github/workflows/`) إلى مستودعك على **GitHub**.
2. انتقل إلى تبويب **Actions** في المستودع.
3. سيبدأ سير العمل **Build Android APK** تلقائياً عند كل `push` إلى `main` أو `master` (ويمكنك تشغيله يدوياً عبر زر **Run workflow**).
4. بعد اكتمال البناء، انزل إلى قسم **Artifacts** في صفحة التشغيل وحمّل ملف **`gestures-debug-apk`** الذي يحتوي على `app-debug.apk`.

### بناء نسخة Release موقّعة (اختياري):
لبناء `app-release.apk` تلقائياً، أضف هذه القيم في **Settings → Secrets and variables → Actions** داخل مستودع GitHub:
- `KEYSTORE_BASE64`: محتوى ملف `.jks` مشفراً بصيغة Base64.
- `STORE_PASSWORD`: كلمة مرور مخزن المفاتيح.
- `KEY_PASSWORD`: كلمة مرور المفتاح (`upload`).
