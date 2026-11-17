# تطبيق إيماءات (Gestures Android App)

تطبيق أندرويد لإيماءات الشاشة متعددة الأصابع والاختصارات السريعة، يعمل فوق أي لانشر مخصص عبر خدمة إمكانية الوصول (AccessibilityService)، الزر العائم، وحواف الشاشة.

## تحويل المشروع إلى APK عبر GitHub Actions

تم تضمين ملف سير العمل في:
`.github/workflows/build-apk.yml`

### خطوات استخراج ملف APK من GitHub:
1. ارفع جميع ملفات المشروع (بما فيها مجلد `.github/workflows/`) إلى مستودعك على **GitHub**.
   - **ملاحظة مهمة عند الرفع**: إذا رفض GitHub رفع مجلد `.github/workflows` عند استخدام أداة دفع أو توكن، تأكد من تفعيل صلاحية **`workflow`** للتوكن، أو ارفع ملف `.github/workflows/build-apk.yml` مباشرة من واجهة GitHub (`Add file -> Create new file`).
2. انتقل إلى تبويب **Actions** في المستودع.
3. سيبدأ سير العمل **Build Android APK** تلقائياً على أي فرع (أو يمكنك تشغيله يدوياً عبر **Run workflow**).
4. بعد اكتمال البناء، يمكنك تحميل `app-debug.apk` من:
   - قسم **Artifacts** (`gestures-debug-apk`) في صفحة التشغيل داخل **Actions**، أو
   - قسم **Releases** (`Latest Gestures APK`) في الصفحة الرئيسية للمستودع.

### بناء نسخة Release موقّعة (اختياري):
لبناء `app-release.apk` تلقائياً، أضف هذه القيم في **Settings → Secrets and variables → Actions** داخل مستودع GitHub:
- `KEYSTORE_BASE64`: محتوى ملف `.jks` مشفراً بصيغة Base64.
- `STORE_PASSWORD`: كلمة مرور مخزن المفاتيح.
- `KEY_PASSWORD`: كلمة مرور المفتاح (`upload`).
