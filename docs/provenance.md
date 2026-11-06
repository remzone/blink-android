# Происхождение

- `native/mobile`, `native/vkturn`, версии Go-зависимостей и fixtures адаптированы из [remzone/blink_ios](https://github.com/remzone/blink_ios), commit `ce8796e9a67769ecb449ba77af18f74b1af63cc6`. Сохранена GPL-3.0-only, исходные LICENSE и `native/vkturn/UPSTREAM.md`.
- `native/vkturn` совместим с [cacggghp/vk-turn-proxy](https://github.com/cacggghp/vk-turn-proxy), commit `e8a96967dc66f3dbd631596ea6a8b9fe03f9be69`. Сохранены DTLS/KCP/smux wire constants; добавлены явный direct режим и vk.ru.
- `launcher/` скопирован из существующего персонального форка CleanYourTV: погода, история, рекомендации и оформление сохранены. Базовый upstream [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), локальный checkout `3c122bc073f7a9a83de251621e6e31d90dd96109`; персональные дополнения исходно были в локальном рабочем дереве. MIT-лицензия лаунчера и лицензии ресурсов сохранены в `launcher/LICENSE*` и подкаталогах ресурсов.
- Новый Android-клиент, адаптация ядра и интеграция распространяются по GPL-3.0-only (корневой LICENSE); отдельный MIT-код лаунчера сохраняет исходные условия. Для зависимостей действуют их собственные лицензии.

Android-сборке Xray/anet требуется `-checklinkname=0` при Go >= 1.23: [инструкция anet](https://github.com/wlynxg/anet), [Xray-core](https://github.com/XTLS/Xray-core). Флаг закреплён в build-native.sh; это не исправление протокола.

## Ручная проверка VK

Используется официальный `com.vk.id.captcha:vkid-captcha:0.0.16` из репозитория VK `https://nexus-external.vkteam.ru/repository/maven/` (MIT согласно POM), Kotlin stdlib 1.9.24 и AndroidX Core 1.13.1 (его требует экран SDK; POM SDK зависимость не объявляет). Способ получения success_token и повторения запроса сверялся с официальным [Error14HandlingInterceptor](https://github.com/VKCOM/vkid-android-sdk/blob/7a18bdc301fb95b94d7968f49cdaf68d8b884414/sdk/core/network/src/main/kotlin/com/vk/id/network/http/captcha/Error14HandlingInterceptor.kt). В SDK Blink не копируются автоматические решатели upstream. Механизм сохранения запроса, срок, привязка результата и UI пульта реализованы в этом репозитории.
