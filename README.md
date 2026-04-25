# CWM

Android tabanli bir dating uygulamasi (tinder-benzeri) projesi.

## Proje Ozeti

- Katmanli yapi: `app`, `data`, `domain`
- Firebase tabanli kimlik dogrulama, veri, depolama ve bildirim akislari
- MVVM + Hilt + RxJava3 mimarisi
- Kart kaydirma, eslesme, sohbet ve profil yonetimi

## Teknoloji ve Mimari

- Kotlin + Android Gradle
- MVVM
- Hilt (DI)
- RxJava3
- Firebase Auth / Firestore / Storage / Messaging / Crashlytics / Analytics
- Glide
- Android Navigation

## Modul Yapisi

- `app`: UI, navigation, viewmodel, Android uygulama katmani
- `data`: repository ve datasource implementasyonlari, Firebase erisimleri
- `domain`: is kurallari ve arayuzler
- `fcm/functions`: Firebase Cloud Functions (Node.js)

## On Kosullar

- Android Studio (guncel bir surum)
- JDK 17 (proje Gradle ayarlari Java 17 hedefli)
- Firebase projesi
- (Opsiyonel) Firebase CLI, eger `fcm/functions` deploy edilecekse

## Kurulum

1. Repoyu klonla ve Android Studio ile ac.
2. Firebase tarafinda Android app olustur ve `google-services.json` dosyasini al.
3. Asagidaki yerel dosyalari olustur (bu dosyalar repoda tutulmuyor/ignore ediliyor):
   - `local.properties`
   - `key.properties`
   - `app/google-services.json`
   - `app/src/main/res/values/misc.xml`
   - `app/src/main/res/values/font_certs.xml`

`key.properties` ornegi:

```properties
FIREBASE_STORAGE_URL="gs://your-project-id.appspot.com"
```

Not: `data/build.gradle` icinde `FIREBASE_STORAGE_URL` degeri `BuildConfig` olarak uretiliyor. Bu degerin dogru formatta (tirnakli string) verilmesi gerekir.

## Uygulamayi Calistirma

```powershell
cd H:\Android\CWM
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

## Firebase Functions (Opsiyonel)

`fcm/functions/package.json` dosyasinda `node: 10` tanimi bulunuyor. Mevcut durum budur; deploy ortaminda surum uyumu kontrol edilmelidir.

```powershell
cd H:\Android\CWM\fcm\functions
npm install
npm run lint
npm run serve
```

Deploy:

```powershell
cd H:\Android\CWM\fcm\functions
npm run deploy
```

## Dizin Agaci

```text
CWM/
|- app/
|- data/
|- domain/
|- fcm/
|  `- functions/
|- media/
|- build.gradle
`- settings.gradle
```

## Katki

- Kucuk ve anlamli commitler tercih edin.
- PR acmadan once en azindan debug build alin.
- Gizli dosyalari (`key.properties`, `local.properties`, `google-services.json`) commit etmeyin.

## Lisans

Bu proje `LICENSE` dosyasindaki kosullarla lisanslanmistir.
