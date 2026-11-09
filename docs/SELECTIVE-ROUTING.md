# Routing and maintenance

Blink 0.6.7 supports panel subscription v3 with `blink.routing.v1`. Open Маршрутизация in the client to inspect the panel policy, add VPN destinations/direct exceptions, choose full VPN or reset to panel defaults. Reconnect to apply changes. Ordinary imported profiles also use the cached policy. Domain rules include subdomains; direct exceptions win.

The packet engine uses FakeDNS and sniffing for domain routing. Other destinations use protected device sockets. Applications with their own encrypted DNS may require IP rules; shared CDN networks can include unrelated services. Selective mode supports standard WireGuard and supported single-outbound Xray profiles. AmneziaWG extensions and standalone TUIC/Hysteria1 require full VPN.

The foreground client polls maintenance status every five seconds using protected HTTPS outside the tunnel. It displays the panel message and refreshes the saved subscription after maintenance ends without restarting an active tunnel. Paid access extension is a manual panel action. Status does not itself extend access.
