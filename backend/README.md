# VocaLingo Backend

Realtime gateway for Android conversation sessions. The APK never contains Google credentials.

## Local Mock Run

```bash
npm install
npm run dev
```

Set Android `local.properties`:

```properties
backendWsUrl=ws://10.0.2.2:8080/ws
```

## Production Run

Deploy this folder with Application Default Credentials or a service account that can call:

- Cloud Speech-to-Text
- Cloud Translation
- Cloud Text-to-Speech

Required environment:

```properties
PORT=8080
VOCALINGO_API_TOKEN=replace-with-a-long-random-token
GOOGLE_CLOUD_PROJECT=vocalingo-503812
GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/google-sa.json
```

Unset `VOCALINGO_MOCK` in production.

## OCI Deployment

The OCI VM uses the existing Caddy container/network for HTTPS/WSS. The backend should not publish a public port; Caddy proxies `https://vocalingo.duckdns.org` and `wss://vocalingo.duckdns.org/ws` to `vocalingo-backend:8080`.

```bash
cp .env.production.example .env.production
mkdir -p secrets
# put the Google service account JSON at secrets/google-sa.json
./deploy-oci.sh
```

The Caddy site block must include:

```caddyfile
vocalingo.duckdns.org {
    encode zstd gzip
    reverse_proxy vocalingo-backend:8080
}
```
