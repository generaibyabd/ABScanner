package com.abdeveloper.abscanner.ui.i18n

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false
)

object AppLocales {
    val LANGUAGES = listOf(
        SupportedLanguage("en", "English", "English"),
        SupportedLanguage("zh", "中文", "Mandarin Chinese"),
        SupportedLanguage("hi", "हिन्दी", "Hindi"),
        SupportedLanguage("es", "Español", "Spanish"),
        SupportedLanguage("ar", "العربية", "Arabic", isRtl = true),
        SupportedLanguage("fr", "Français", "French"),
        SupportedLanguage("bn", "বাংলা", "Bengali"),
        SupportedLanguage("pt", "Português", "Portuguese"),
        SupportedLanguage("ru", "Русский", "Russian"),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian")
    )

    fun getLanguage(code: String): SupportedLanguage {
        return LANGUAGES.find { it.code.equals(code, ignoreCase = true) } ?: LANGUAGES.first()
    }
}

class Strings(val lang: String = "en") {
    val tabScan = when (lang) {
        "zh" -> "扫描"
        "hi" -> "स्कैन"
        "es" -> "Escanear"
        "ar" -> "مسح"
        "fr" -> "Scanner"
        "bn" -> "স্ক্যান"
        "pt" -> "Escanear"
        "ru" -> "Сканер"
        "id" -> "Pindai"
        else -> "Scan"
    }

    val tabCreate = when (lang) {
        "zh" -> "生成"
        "hi" -> "बनाएं"
        "es" -> "Crear"
        "ar" -> "إنشاء"
        "fr" -> "Créer"
        "bn" -> "তৈরি করুন"
        "pt" -> "Criar"
        "ru" -> "Создать"
        "id" -> "Buat"
        else -> "Create"
    }

    val tabSettings = when (lang) {
        "zh" -> "设置"
        "hi" -> "सेटिंग्स"
        "es" -> "Ajustes"
        "ar" -> "الإعدادات"
        "fr" -> "Paramètres"
        "bn" -> "সেটিংস"
        "pt" -> "Configurações"
        "ru" -> "Настройки"
        "id" -> "Pengaturan"
        else -> "Settings"
    }

    val scanTitle = when (lang) {
        "zh" -> "ABScanner 条码扫描"
        "hi" -> "ABScanner स्कैनर"
        "es" -> "ABScanner Escáner"
        "ar" -> "ماسح ABScanner"
        "fr" -> "Scanner ABScanner"
        "bn" -> "ABScanner স্ক্যানার"
        "pt" -> "ABScanner Leitor"
        "ru" -> "ABScanner Сканер"
        "id" -> "Pemindai ABScanner"
        else -> "ABScanner"
    }

    val cameraPermissionTitle = when (lang) {
        "zh" -> "需要相机权限"
        "hi" -> "कैमरा अनुमति आवश्यक है"
        "es" -> "Permiso de cámara requerido"
        "ar" -> "مطلوب إذن الكاميرا"
        "fr" -> "Autorisation de la caméra requise"
        "bn" -> "ক্যামেরা অনুমতি প্রয়োজন"
        "pt" -> "Permissão de câmera necessária"
        "ru" -> "Требуется доступ к камере"
        "id" -> "Izin kamera diperlukan"
        else -> "Camera Permission Required"
    }

    val cameraPermissionDesc = when (lang) {
        "zh" -> "ABScanner 需要相机权限来扫描二维码和条形码。不会捕获任何个人照片或上传到网络。"
        "hi" -> "ABScanner को QR और बारकोड स्कैन करने के लिए कैमरा अनुमति की आवश्यकता है। कोई व्यक्तिगत डेटा अपलोड नहीं किया जाता है।"
        "es" -> "ABScanner necesita la cámara para escanear códigos QR y barras. Totalmente sin conexión."
        "ar" -> "يحتاج ABScanner إلى الكاميرا لمسح رموز الاستجابة السريعة والباركود محلياً دون أي خوادم."
        "fr" -> "ABScanner a besoin de la caméra pour scanner les codes QR et codes-barres en mode hors ligne."
        "bn" -> "কিউআর এবং বারকোড স্ক্যান করতে ক্যামেরার অনুমতি দিন। সম্পূর্ণ অফলাইন এবং নিরাপদ।"
        "pt" -> "O ABScanner precisa da câmera para ler códigos QR e de barras de forma 100% offline."
        "ru" -> "ABScanner использует камеру для сканирования QR и штрихкодов в полностью автономном режиме."
        "id" -> "ABScanner memerlukan kamera untuk memindai kode QR dan barcode secara offline."
        else -> "ABScanner needs camera access to scan QR codes and barcodes. Completely offline and private."
    }

    val grantPermission = when (lang) {
        "zh" -> "授予相机权限"
        "hi" -> "अनुमति दें"
        "es" -> "Conceder permiso"
        "ar" -> "منح الإذن"
        "fr" -> "Accorder l'accès"
        "bn" -> "অনুমতি দিন"
        "pt" -> "Conceder permissão"
        "ru" -> "Разрешить доступ"
        "id" -> "Berikan Izin"
        else -> "Grant Camera Permission"
    }

    val batchMode = when (lang) {
        "zh" -> "批量模式"
        "hi" -> "बैच मोड"
        "es" -> "Modo por lotes"
        "ar" -> "وضع الدفعات"
        "fr" -> "Mode par lots"
        "bn" -> "ব্যাচ মোড"
        "pt" -> "Modo lote"
        "ru" -> "Пакетный режим"
        "id" -> "Mode Batch"
        else -> "Batch Mode"
    }

    val pickFromGallery = when (lang) {
        "zh" -> "从相册导入"
        "hi" -> "गैलरी से चुनें"
        "es" -> "Elegir de galería"
        "ar" -> "اختر من المعرض"
        "fr" -> "Importer de la galerie"
        "bn" -> "গ্যালারি থেকে নিন"
        "pt" -> "Escolher da galeria"
        "ru" -> "Выбрать из галереи"
        "id" -> "Pilih dari Galeri"
        else -> "Scan from Gallery"
    }

    val noCodeFoundInImage = when (lang) {
        "zh" -> "在所选图片中未找到二维码或条码。请裁剪更近或提高对比度后重试。"
        "hi" -> "चुनी गई छवि में कोई कोड नहीं मिला। कृपया ज़ूम करें या अधिक स्पष्ट फोटो आज़माएं।"
        "es" -> "No se encontró ningún código en esta imagen. Intente recortar más cerca o mejorar el contraste."
        "ar" -> "لم يتم العثور على رمز في هذه الصورة. يرجى قص الصورة أقرب أو زيادة التباين."
        "fr" -> "Aucun code trouvé dans cette image. Recadrez de plus près ou essayez une image plus nette."
        "bn" -> "এই ছবিতে কোনও কোড পাওয়া যায়নি। ক্রপ করে আবার চেষ্টা করুন।"
        "pt" -> "Nenhum código encontrado nesta imagem. Tente recortar mais perto ou melhorar o contraste."
        "ru" -> "Код на изображении не найден. Попробуйте обрезать фото ближе к коду."
        "id" -> "Tidak ada kode yang ditemukan pada gambar ini. Silakan potong lebih dekat atau coba gambar lain."
        else -> "No code found in this image. Tip: crop closer or improve contrast and try again."
    }

    val copy = when (lang) {
        "zh" -> "复制"
        "hi" -> "कॉपी"
        "es" -> "Copiar"
        "ar" -> "نسخ"
        "fr" -> "Copier"
        "bn" -> "কপি"
        "pt" -> "Copiar"
        "ru" -> "Копировать"
        "id" -> "Salin"
        else -> "Copy"
    }

    val share = when (lang) {
        "zh" -> "分享"
        "hi" -> "शेयर"
        "es" -> "Compartir"
        "ar" -> "مشاركة"
        "fr" -> "Partager"
        "bn" -> "শেয়ার"
        "pt" -> "Compartilhar"
        "ru" -> "Поделиться"
        "id" -> "Bagikan"
        else -> "Share"
    }

    val save = when (lang) {
        "zh" -> "收藏"
        "hi" -> "सहेजें"
        "es" -> "Guardar"
        "ar" -> "حفظ"
        "fr" -> "Enregistrer"
        "bn" -> "সংরক্ষণ"
        "pt" -> "Salvar"
        "ru" -> "Сохранить"
        "id" -> "Simpan"
        else -> "Save"
    }

    val saved = when (lang) {
        "zh" -> "已收藏"
        "hi" -> "सहेजा गया"
        "es" -> "Guardado"
        "ar" -> "محفوظ"
        "fr" -> "Enregistré"
        "bn" -> "সংরক্ষিত"
        "pt" -> "Salvo"
        "ru" -> "Сохранено"
        "id" -> "Tersimpan"
        else -> "Saved"
    }

    val rawContent = when (lang) {
        "zh" -> "原始数据"
        "hi" -> "मूल डेटा"
        "es" -> "Datos sin procesar"
        "ar" -> "البيانات الأصلية"
        "fr" -> "Données brutes"
        "bn" -> "কাঁচা ডেটা"
        "pt" -> "Dados brutos"
        "ru" -> "Исходные данные"
        "id" -> "Data Mentah"
        else -> "Raw Content"
    }

    val safetyAnalysis = when (lang) {
        "zh" -> "离线安全分析"
        "hi" -> "सुरक्षा विश्लेषण"
        "es" -> "Análisis de seguridad"
        "ar" -> "تحليل الأمان بدون إنترنت"
        "fr" -> "Analyse de sécurité hors ligne"
        "bn" -> "নিরাপত্তা বিশ্লেষণ"
        "pt" -> "Análise de segurança offline"
        "ru" -> "Анализ безопасности (офлайн)"
        "id" -> "Analisis Keamanan Offline"
        else -> "Offline Safety Analysis"
    }

    val safeUrl = when (lang) {
        "zh" -> "安全 (无已知风险)"
        "hi" -> "सुरक्षित (कोई जोखिम नहीं)"
        "es" -> "Seguro (sin riesgos detectados)"
        "ar" -> "آمن (لم يتم العثور على مخاطر)"
        "fr" -> "Sûr (aucun risque détecté)"
        "bn" -> "নিরাপদ"
        "pt" -> "Seguro (sem riscos detectados)"
        "ru" -> "Безопасно (угроз не обнаружено)"
        "id" -> "Aman (tidak ada risiko terdeteksi)"
        else -> "Safe (No detected risks)"
    }

    val suspiciousUrl = when (lang) {
        "zh" -> "警告: 潜在可疑链接"
        "hi" -> "चेतावनी: संदिग्ध लिंक"
        "es" -> "Advertencia: enlace potencialmente sospechoso"
        "ar" -> "تحذير: رابط مشبوه محتمل"
        "fr" -> "Attention : lien potentiellement suspect"
        "bn" -> "সতর্কতা: সন্দেহজনক লিঙ্ক"
        "pt" -> "Aviso: link potencialmente suspeito"
        "ru" -> "Внимание: подозрительная ссылка"
        "id" -> "Peringatan: Tautan mencurigakan"
        else -> "Warning: Suspicious Link"
    }

    val dangerUrl = when (lang) {
        "zh" -> "危险: 高危钓鱼风险"
        "hi" -> "खतरा: फ़िशिंग का जोखिम"
        "es" -> "Peligro: alto riesgo de phishing"
        "ar" -> "خطر: تهديد تصيد احتيالي عالي"
        "fr" -> "Danger : risque élevé d'hameçonnage"
        "bn" -> "বিপদ: ফিশিং ঝুঁকি"
        "pt" -> "Perigo: alto risco de phishing"
        "ru" -> "Опасно: высокая вероятность фишинга"
        "id" -> "Bahaya: Risiko phishing tinggi"
        else -> "Danger: High Phishing Risk"
    }

    val generateCode = when (lang) {
        "zh" -> "立即生成二维码/条码"
        "hi" -> "कोड बनाएं"
        "es" -> "Generar código"
        "ar" -> "توليد الرمز"
        "fr" -> "Générer le code"
        "bn" -> "কোড তৈরি করুন"
        "pt" -> "Gerar código"
        "ru" -> "Создать код"
        "id" -> "Buat Kode"
        else -> "Generate Code"
    }

    val savePng = when (lang) {
        "zh" -> "保存为 PNG 图片"
        "hi" -> "PNG के रूप में सहेजें"
        "es" -> "Guardar PNG"
        "ar" -> "حفظ كصورة PNG"
        "fr" -> "Enregistrer en PNG"
        "bn" -> "PNG সংরক্ষণ করুন"
        "pt" -> "Salvar imagem PNG"
        "ru" -> "Сохранить как PNG"
        "id" -> "Simpan sebagai PNG"
        else -> "Save PNG Image"
    }

    val scannabilityVerified = when (lang) {
        "zh" -> "可读性验证通过"
        "hi" -> "स्कैन सत्यापन सफल"
        "es" -> "Legibilidad verificada"
        "ar" -> "تم التحقق من إمكانية المسح بنجاح"
        "fr" -> "Scannabilité vérifiée avec succès"
        "bn" -> "স্ক্যানযোগ্যতা যাচাই সম্পন্ন"
        "pt" -> "Leitura verificada com sucesso"
        "ru" -> "Сканируемость проверена"
        "id" -> "Keterbacaan terverifikasi"
        else -> "Scannability Verified"
    }

    val appearance = when (lang) {
        "zh" -> "外观与主题"
        "hi" -> "उपस्थिति और थीम"
        "es" -> "Apariencia y tema"
        "ar" -> "المظهر والسمة"
        "fr" -> "Apparence et thème"
        "bn" -> "চেহারা এবং থিম"
        "pt" -> "Aparência e tema"
        "ru" -> "Внешний вид и тема"
        "id" -> "Tampilan & Tema"
        else -> "Appearance & Theme"
    }

    val language = when (lang) {
        "zh" -> "语言 (Language)"
        "hi" -> "भाषा (Language)"
        "es" -> "Idioma (Language)"
        "ar" -> "اللغة (Language)"
        "fr" -> "Langue (Language)"
        "bn" -> "ভাষা (Language)"
        "pt" -> "Idioma (Language)"
        "ru" -> "Язык (Language)"
        "id" -> "Bahasa (Language)"
        else -> "Language"
    }

    val history = when (lang) {
        "zh" -> "历史记录"
        "hi" -> "इतिहास"
        "es" -> "Historial"
        "ar" -> "سجل المسح"
        "fr" -> "Historique"
        "bn" -> "ইতিহাস"
        "pt" -> "Histórico"
        "ru" -> "История"
        "id" -> "Riwayat"
        else -> "Scan History"
    }

    val savedCodes = when (lang) {
        "zh" -> "收藏列表"
        "hi" -> "सहेजे गए कोड"
        "es" -> "Códigos guardados"
        "ar" -> "الرموز المحفوظة"
        "fr" -> "Codes enregistrés"
        "bn" -> "সংরক্ষিত কোড"
        "pt" -> "Códigos salvos"
        "ru" -> "Сохранённые коды"
        "id" -> "Kode Tersimpan"
        else -> "Saved Codes"
    }

    val privacyPolicy = when (lang) {
        "zh" -> "隐私政策"
        "hi" -> "गोपनीयता नीति"
        "es" -> "Política de privacidad"
        "ar" -> "سياسة الخصوصية"
        "fr" -> "Politique de confidentialité"
        "bn" -> "গোপনীয়তা নীতি"
        "pt" -> "Política de privacidade"
        "ru" -> "Политика конфиденциальности"
        "id" -> "Kebijakan Privasi"
        else -> "Privacy Policy"
    }

    val termsOfUse = when (lang) {
        "zh" -> "使用条款"
        "hi" -> "उपयोग की शर्तें"
        "es" -> "Términos de uso"
        "ar" -> "شروط الاستخدام"
        "fr" -> "Conditions d'utilisation"
        "bn" -> "ব্যবহারের শর্তাবলী"
        "pt" -> "Termos de uso"
        "ru" -> "Условия использования"
        "id" -> "Ketentuan Penggunaan"
        else -> "Terms of Use"
    }
}
