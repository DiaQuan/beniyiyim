# Ben İyiyim

> "Herhangi birimiz bir süre alkol aldıktan sonra 'ben iyiyim' diyorsa muhtemelen iyi değildir."

Alkol alımı sırasında kullanıcının bilişsel/motor durumunu periyodik testlerle ölçen,
güven skoru düştükçe kademeli müdahalelerde bulunan (uygulama kısıtlama, ayık arkadaşa
bildirim, taksı önerisi) bir Android güvenlik uygulaması.

## ⚠️ Proje Durumu

Bu bir **MVP (Minimum Viable Product) iskeletidir**. Derlenebilir bir proje yapısı,
temel iş mantığı (güven skoru motoru, itibar/reputation sistemi, test mekanikleri) ve
CI/CD pipeline'ı içerir. Aşağıdaki bölümlerde neyin tamamlandığı, neyin placeholder
olduğu ve production'a geçmeden önce nelerin yapılması gerektiği açıkça belirtilmiştir.

## Mimari Özeti

- **Dil / Platform:** Kotlin, Android native (minSdk 26, targetSdk 34)
- **Backend:** Firebase (Firestore, Auth, Cloud Messaging)
- **CI/CD:** GitHub Actions (`.github/workflows/`)

### Ana Bileşenler

| Bileşen | Dosya | Açıklama |
|---|---|---|
| Güven Skoru Motoru | `core/TrustScoreEngine.kt` | Test sonuçlarından 0-100 arası güven skoru hesaplar (EWMA) |
| İtibar Motoru | `core/ReputationEngine.kt` | Troll/kötüye kullanım önleme — art arda iptal edenlerin otomatik taksı yetkisini kısıtlar |
| Erişilebilirlik Servisi | `accessibility/AppMonitorAccessibilityService.kt` | Kısıtlanan uygulamaların (Instagram, SMS vb.) açılışını tespit eder |
| Test Aktivitesi | `test/SobrietyTestActivity.kt` | Tam ekran overlay testi (reaksiyon, matematik, gecikmeli hafıza) |
| Gece Servisi | `core/NightSessionService.kt` | Foreground service, periyodik test tetikleyici |
| Kriz Akışı | `core/CrisisFlowManager.kt` | Ayık arkadaş bildirimi + taksı önerisi (otomatik sipariş YOK) |

## ✅ Tamamlanan Kısımlar

- Gradle proje yapısı (root + app modülü), Kotlin DSL
- Tüm AndroidManifest izinleri ve bileşen tanımları
- Güven skoru hesaplama algoritması + **birim testleri** (`app/src/test/`)
- İtibar/reputation sistemi + birim testleri
- Accessibility Service ile uygulama izleme
- Tam ekran test overlay'i (3 test tipi: reaksiyon süresi, matematik, gecikmeli kelime hatırlama)
- Onboarding akışı (izin izinleri tek tek açıklanarak istenir)
- Firebase Firestore ile temel veri modelleri
- Kriz akışı: ayık arkadaş onayı + taksı derin linki (otomatik sipariş YOK, bilinçli tasarım kararı)
- GitHub Actions: build, unit test, lint, debug APK + imzalı release AAB workflow'ları

## 🚧 MVP'de Placeholder / Basitleştirilmiş Kalan Kısımlar

Bunlar **bilerek** basitleştirilmiştir çünkü gerçek entegrasyon harici servis
hesapları / sözleşmeler gerektirir. Kod, nereye ne ekleneceğini yorum satırlarıyla işaret eder:

1. **Taksı API entegrasyonu:** Şu an `bitaksi://` gibi bir deep-link açıyor. Gerçek
   otomatik sipariş için BiTaksi/Uber Business API ortaklığı gerekir.
2. **Firestore `await()` çağrıları:** `CrisisFlowManager.fetchReputation()` içinde
   gerçek asenkron okuma yerine placeholder var — `kotlinx-coroutines-play-services`
   eklenip `Task<T>.await()` kullanılmalı.
3. **Arkadaş yanıtını dinleme:** Şu an sabit 2 dakika bekleyip `NO_RESPONSE` dönüyor.
   Gerçek sürümde Firestore snapshot listener veya Cloud Function + FCM data message
   ile arkadaşın "iptal" tıklamasını gerçek zamanlı dinlemek gerekir.
4. **Ses/diksiyon testi (Faz 2):** `TestType.VOICE_DICTION` enum'da tanımlı ama
   henüz bir `SobrietyTestSpec` / scorer implementasyonu yok. Ses kaydı KVKK
   açısından ayrı ve daha sıkı bir rıza akışı gerektirir.
5. **Mekan/Grup modu ("Aynı Masadayız"), Vale entegrasyonu, Gece Kapsülü:**
   Veri modelinde (`NightGroup`) yer var ama UI/akış henüz yazılmadı — Faz 2 kapsamı.
6. **Launcher icon:** Şu an düz renkli adaptive icon placeholder'ı var,
   gerçek marka ikonuyla değiştirilmeli.

## Kurulum ve Geliştirme

### Gereksinimler
- JDK 17
- Android Studio (Koala veya üzeri) — yerel geliştirme için
- Bir Firebase projesi (Firestore + Auth + Cloud Messaging aktif)

### Firebase Kurulumu
1. [Firebase Console](https://console.firebase.google.com)'da yeni proje oluştur
2. Paket adı olarak `com.beniyiyim.app` ekle
3. İndirdiğin `google-services.json` dosyasını `app/google-services.json` olarak koy
   (bu dosya `.gitignore`'da olduğu için commit edilmez)
4. CI'da kullanmak için: dosyanın base64 halini GitHub Secrets'a
   `FIREBASE_GOOGLE_SERVICES_JSON` adıyla ekle:
   ```bash
   base64 -i app/google-services.json | pbcopy   # macOS
   base64 -w0 app/google-services.json           # Linux
   ```

### Yerel Derleme
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

> Not: `gradlew` script'i ve wrapper JAR'ı bu repoda commit edilmemiştir (binary dosya).
> CI workflow'u bunu `gradle wrapper --gradle-version 8.7` komutuyla otomatik üretir.
> Yerel geliştirmede Android Studio projeyi açtığında wrapper'ı otomatik kuracaktır,
> ya da manuel olarak `gradle wrapper --gradle-version 8.7` çalıştırabilirsin.

### Release İmzalama (GitHub Actions)
`android-release.yml` workflow'u şu GitHub Secrets'ları kullanır:
- `RELEASE_KEYSTORE_BASE64` — keystore dosyasının base64 hali
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Bir `v*.*.*` formatında tag push edildiğinde otomatik olarak imzalı AAB üretir ve
GitHub Release oluşturur.

## Güvenlik / Yasal Notlar (önemli)

- Konum paylaşımı, üçüncü kişiye bildirim gönderme ve ses kaydı gibi özellikler
  **KVKK kapsamında açık ve ayrıştırılmış rıza** gerektirir — onboarding akışında
  her izin ayrı ayrı açıklanarak istenir, tek bir "kabul ediyorum" ile geçiştirilmez.
- Sistem **hiçbir zaman kullanıcı adına otomatik taksı siparişi vermez** — bu bilinçli
  bir tasarım kararıdır (hem trolleme riskini azaltır hem de "rızam olmadan sipariş
  verildi" şikayetlerini önler). Son dokunuşu her zaman bir insan yapar.
- Bankacılık/ödeme ekranlarına overlay bindirme gibi özellikler bu MVP'ye **bilerek
  dahil edilmemiştir** çünkü bankacılık uygulamaları genelde overlay tespiti yapıp
  kendini kapatır; bu, MVP kapsamının dışında tutulmuş riskli bir alandır.

## Faz 2 / Faz 3 Fikir Havuzu (henüz kodlanmadı)

- Mekan B2B güvenlik paneli (anonim ısı haritası)
- Vale/araç teslim entegrasyonu
- Ses/diksiyon analizi testi
- Akıllı bileklik entegrasyonu (düşme algılama — Apple Watch/Galaxy Watch native API'leri tercih edilmeli)
- Eve Vardım doğrulaması (Geofence + WiFi SSID tespiti)
- Gece Sonu Raporu / "Kara Kutu"
- Gizli Gece Kapsülü (ertesi gün 12:00'de açılan içerik)
- Gamification: rozetler, streak sistemi
