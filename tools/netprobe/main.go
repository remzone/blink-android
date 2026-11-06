// SPDX-License-Identifier: GPL-3.0-only
// Standalone on-device HTTPS diagnostic; output may contain the egress IP.
package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"flag"
	"io"
	"net"
	"net/http"
	"net/http/httptrace"
	"os"
	"path/filepath"
	"strings"
	"sync/atomic"
	"time"
)

func main() {
	dnsTCP := flag.Bool("dns-tcp", false, "Send diagnostic DNS through TCP rather than UDP")
	host := flag.String("host", "www.cloudflare.com", "HTTPS diagnostic host (no credentials)")
	path := flag.String("path", "/cdn-cgi/trace", "HTTPS path")
	flag.Parse()
	roots := x509.NewCertPool()
	files, _ := filepath.Glob("/system/etc/security/cacerts/*")
	for _, f := range files {
		b, _ := os.ReadFile(f)
		roots.AppendCertsFromPEM(b)
	}
	resolver := &net.Resolver{PreferGo: true, Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
		if *dnsTCP {
			network = "tcp"
		}
		return (&net.Dialer{Timeout: 4 * time.Second}).DialContext(ctx, network, "1.1.1.1:53")
	}}
	for _, family := range []string{"ip4", "ip6", "ip"} {
		ctx, cancel := context.WithTimeout(context.Background(), 12*time.Second)
		start := time.Now()
		r := map[string]any{"family": family, "dns_tcp": *dnsTCP}
		ips, e := resolver.LookupIP(ctx, family, *host)
		r["dns_ms"] = time.Since(start).Milliseconds()
		if e != nil || len(ips) == 0 {
			r["error"] = "dns_failed"
			json.NewEncoder(os.Stdout).Encode(r)
			cancel()
			continue
		}
		tr := &http.Transport{DisableKeepAlives: true, TLSClientConfig: &tls.Config{RootCAs: roots, ServerName: *host}, TLSHandshakeTimeout: 6 * time.Second, DialContext: func(ctx context.Context, _, _ string) (net.Conn, error) {
			if family == "ip" {
				return (&net.Dialer{Timeout: 6 * time.Second, Resolver: resolver}).DialContext(ctx, "tcp", net.JoinHostPort(*host, "443"))
			}
			return (&net.Dialer{Timeout: 6 * time.Second}).DialContext(ctx, "tcp", net.JoinHostPort(ips[0].String(), "443"))
		}}
		var phase atomic.Value
		phase.Store("tcp")
		var tlsMS, firstByteMS atomic.Int64
		var mark time.Time
		trace := &httptrace.ClientTrace{TLSHandshakeStart: func() { phase.Store("tls"); mark = time.Now() }, TLSHandshakeDone: func(_ tls.ConnectionState, e error) {
			tlsMS.Store(time.Since(mark).Milliseconds())
			if e == nil {
				phase.Store("http")
			}
		}, GotFirstResponseByte: func() { firstByteMS.Store(time.Since(start).Milliseconds()) }}
		req, _ := http.NewRequestWithContext(httptrace.WithClientTrace(ctx, trace), "GET", "https://"+*host+*path, nil)
		resp, e := (&http.Client{Transport: tr}).Do(req)
		if e != nil {
			r["error_phase"] = phase.Load()
			r["error"] = e.Error()
		} else {
			body, e := io.ReadAll(io.LimitReader(resp.Body, 8192))
			resp.Body.Close()
			r["http_status"] = resp.StatusCode
			r["body_read_ok"] = e == nil
			for _, line := range strings.Split(string(body), "\n") {
				if strings.HasPrefix(line, "ip=") {
					r["egress"] = strings.TrimPrefix(line, "ip=")
				}
			}
		}
		r["tls_ms"] = tlsMS.Load()
		r["first_byte_ms"] = firstByteMS.Load()
		r["total_ms"] = time.Since(start).Milliseconds()
		json.NewEncoder(os.Stdout).Encode(r)
		tr.CloseIdleConnections()
		cancel()
	}
}
