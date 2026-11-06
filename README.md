# Blink for Android TV

Android TV клиент панели Vokop и персональный лаунчер с разделом **Сетевые настройки**.

- Подключение/отключение туннеля пультом, подписка, выбор профиля, срок действия и обновление ссылки.
- VK TURN → DTLS → KCP → smux → VLESS. Отдельный ручной **прямой UDP** режим проверяет сервер без VK. Автоматического перехода между режимами нет.
- Диагностика различает готовность транспорта и успешный HTTPS-запрос через Android VPN; показывает счётчики, внешний IP и время запроса.
- Wi-Fi/Ethernet и локальная сеть остаются доступны. VPN не стартует при загрузке ТВ. При CAPTCHA или отсутствии соединения 45 секунд VPN закрывается и возвращается обычная сеть.

`app/` — клиент `com.remzone.blink.tv`, `launcher/` — обновление персонального `com.gathin.tvhome`. Лаунчер не получает INTERNET и ключи подписки. Он читает состояние и переключает сервис через provider с permission уровня signature. Оба APK должны быть подписаны одним ключом.

Android 8.0+ / API 26+, сборки ARMv7 и ARM64. Интерфейс рассчитан на горизонтальный экран и пульт.

## Сборка

Нужны JDK 17, Android SDK 34, Build Tools 34.0.0, NDK 27.2.12479018, Go 1.27.x. Версии Go-зависимостей и gomobile закреплены в `native/go.mod` и `go.sum`; Gradle wrapper — 8.9, AGP — 8.5.2.

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
export PATH="$JAVA_HOME/bin:/path/to/go/bin:$PATH"
# Для обновления существующего лаунчера используйте прежний signing key.
# Текущая локальная сборка обоих модулей использует debug.keystore из ANDROID_USER_HOME.
export ANDROID_USER_HOME=/path/to/existing/android-user
./tools/build.sh
(cd native && go test -race ./...)
```

Скрипт создаёт локальный `native/blinkcore.aar`, выполняет unit tests, lint и сборку обоих APK:

```
app/build/outputs/apk/release/app-release.apk
launcher/build/outputs/apk/release/launcher-release.apk
```

APK, AAR, signing key, кеши, реальные подписки и отчёты с идентификаторами устройств исключены из Git. GitHub Actions и GitHub Releases не используются. Для распространения вне личного стенда настройте собственную release-подпись в обоих модулях; сохраните прежний ключ для обновления уже установленного Home.

## Установка и первый запуск

```bash
adb -s TV_ADB_ADDRESS install -r app/build/outputs/apk/release/app-release.apk
adb -s TV_ADB_ADDRESS install -r launcher/build/outputs/apk/release/launcher-release.apk
```

Не удаляйте установленный лаунчер: `install -r` с прежней подписью сохраняет настройки, оформление и историю. Перед обновлением сохраните текущий APK. Смена Home, отключение системных приложений и перезагрузка для установки не нужны.

Откройте сетевую кнопку лаунчера → «Подписка и профили Vokop» → «Добавить / заменить ссылку». Нужна scoped HTTPS-ссылка вида `https://panel.example.org/vokop-panel/api/vokop/v1/subscriptions/TOKEN` из панели. Затем выберите режим, нажмите «Подключить туннель» и подтвердите системное разрешение VPN. «Проверить интернет через туннель» выполняет HTTPS-запрос к Cloudflare trace; внешний IP должен соответствовать выходу сервера.

Ссылка и документ подписки шифруются AES-GCM ключом AndroidKeyStore; резервное копирование и перенос исключены. Административный пароль панели клиенту не нужен. Экран показывает срок и версию подписки; денежный баланс панель этим API не отдаёт.

## Ограничения протокола

VK может запросить CAPTCHA при выдаче TURN-реквизитов. Клиент сообщает об этом и останавливает VPN; автоматического обхода CAPTCHA нет. Прямой режим доказывает работу сервера и Android VPN, но не прохождение трафика через VK.

Сохранена совместимость с сервером `vk-turn-proxy` на commit `e8a96967dc66f3dbd631596ea6a8b9fe03f9be69`: upstream DTLS не проверяет подлинность сертификата сервера, внутренний VLESS использует `encryption=none`. Это экспериментальный транспорт; не следует считать его заменой аутентифицированному сквозному TLS. HTTPS приложений продолжает проверять сертификаты самостоятельно.

Подробности: [архитектура и контракт](docs/architecture.md), [происхождение и лицензии](docs/provenance.md), [проверка](docs/validation.md).
