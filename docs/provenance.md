# Происхождение

- `native/mobile`, `native/vkturn`, версии Go-зависимостей и fixtures адаптированы из [remzone/blink_ios](https://github.com/remzone/blink_ios), commit `ce8796e9a67769ecb449ba77af18f74b1af63cc6`. Сохранена GPL-3.0-only, исходные LICENSE и `native/vkturn/UPSTREAM.md`.
- `native/vkturn` совместим с [cacggghp/vk-turn-proxy](https://github.com/cacggghp/vk-turn-proxy), commit `e8a96967dc66f3dbd631596ea6a8b9fe03f9be69`. Сохранены DTLS/KCP/smux wire constants; добавлены явный direct режим и vk.ru.
- `launcher/` скопирован из существующего персонального форка CleanYourTV: погода, история, рекомендации и оформление сохранены. Базовый upstream [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), локальный checkout `3c122bc073f7a9a83de251621e6e31d90dd96109`; персональные дополнения исходно были в локальном рабочем дереве. MIT-лицензия лаунчера и лицензии ресурсов сохранены в `launcher/LICENSE*` и подкаталогах ресурсов.
- Новый Android-клиент, адаптация ядра и интеграция распространяются по GPL-3.0-only (корневой LICENSE); отдельный MIT-код лаунчера сохраняет исходные условия. Для зависимостей действуют их собственные лицензии.

Android-сборке Xray/anet требуется `-checklinkname=0` при Go >= 1.23: [инструкция anet](https://github.com/wlynxg/anet), [Xray-core](https://github.com/XTLS/Xray-core). Флаг закреплён в build-native.sh; это не исправление протокола.

## Ручная проверка VK

Используется официальный `com.vk.id.captcha:vkid-captcha:0.0.16` из репозитория VK `https://nexus-external.vkteam.ru/repository/maven/` (MIT согласно POM), Kotlin stdlib 1.9.24 и AndroidX Core 1.13.1 (его требует экран SDK; POM SDK зависимость не объявляет). Способ получения success_token и повторения запроса сверялся с официальным [Error14HandlingInterceptor](https://github.com/VKCOM/vkid-android-sdk/blob/7a18bdc301fb95b94d7968f49cdaf68d8b884414/sdk/core/network/src/main/kotlin/com/vk/id/network/http/captcha/Error14HandlingInterceptor.kt). В SDK Blink не копируются автоматические решатели upstream. Механизм сохранения запроса, срок, привязка результата и UI пульта реализованы в этом репозитории.

## Обычные VPN · 0.4.0

- Xray-core закреплён на `0afec66085a85191e9dd2a4a6646c7c9a94688d1`
  (`native/go.mod`), MPL-2.0; официальные [документы](https://xtls.github.io/).
- WG и AmneziaWG используют официальный MIT backend
  [amnezia-vpn/amneziawg-go](https://github.com/amnezia-vpn/amneziawg-go),
  v3.1.20260828, commit `b5928efb6ca19f0153958460c3d141f04abc5c2e`.
  Пакетный адаптер и защищённый mobile bind написаны в `native/mobile/wg_backend.go`.
  Официальный `wireguard-go` применяется как независимый peer в тесте обычного WG.
- URI-парсер в `native/share` адаптирован из MIT
  [XTLS/libXray](https://github.com/XTLS/libXray),
  commit `7fe8528063e54343af8ba9a37431afc56000de21`.
  Сохранён `native/share/LICENSE`; GPL wrappers добавляют строгую валидацию,
  лимиты, legacy VMess/SS import и ошибки без реквизитов.
- Зеркало ядра для iOS: `blink_ios/Native`, замена только module import prefix.
  `blink_ios/tools/sync-native.py --check` проверяет отсутствие расхождений.

Все версии транзитивных зависимостей закреплены в go.mod/go.sum. Новые MIT
компоненты не меняют GPL-лицензию интеграции и условия других зависимостей.

## Дополнения 0.5.0

- Официальный [SagerNet/sing-quic](https://github.com/SagerNet/sing-quic), v0.7.1, GPL-3.0-or-later: TUIC v5 и legacy Hysteria v1. Все транзитивные версии закреплены в go.mod/go.sum.
- `gopkg.in/yaml.v3` v3.0.1, MIT/Apache-2.0: ограниченный parser узлов Clash. Формат Clash не означает использование его routing engine.
- `native/resources/`: geoip/geosite из официального дистрибутива Xray, сохранены LICENSE и UPSTREAM.md с SHA256 архива/файлов. Это данные для пользовательских JSON routing rules.
- `native/mobile/testdata/panel-exports.json`: синтетический контракт реальных экспортёров remzone/vokop-panel; живых реквизитов нет.
