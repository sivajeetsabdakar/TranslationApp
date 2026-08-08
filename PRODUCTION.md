# VocaLingo Production Notes

## Android Release

- Release application id: `com.vocalingo.app`
- Release signing is read from ignored `keystore.properties`.
- Template: `keystore.properties.sample`
- Local ignored files that must be backed up securely:
  - `release-keystore.jks`
  - `keystore.properties`
  - `app/google-services.json`

Build commands:

```powershell
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

Release outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## Firebase

Firebase is wired conditionally. If `app/google-services.json` exists, the app applies:

- Google Services Gradle plugin
- Firebase Crashlytics Gradle plugin
- Firebase Analytics SDK
- Firebase Crashlytics SDK

Firebase app registered:

- Project: `vocalingo-503812`
- Android package: `com.vocalingo.app`

## Backend

Production endpoint:

- HTTPS health: `https://vocalingo.duckdns.org/health`
- WebSocket: `wss://vocalingo.duckdns.org/ws`

OCI deployment is compose-managed from:

- `/home/ubuntu/vocalingo-backend/docker-compose.oci.yml`

The backend should not publish a public app port. Caddy is the only public ingress on 80/443 and proxies to `vocalingo-backend:8080` on Docker network `whatsnew_default`.

## Google Cloud

Project: `vocalingo-503812`

Required enabled APIs:

- Speech-to-Text
- Cloud Translation
- Cloud Text-to-Speech
- Firebase
- Firebase Crashlytics
- Firebase Installations
- Cloud Monitoring
- Cloud Billing Budget API

Billing is enabled on billing account `0129D8-5825E7-3968E0`.

Budget alert:

- Name: `VocaLingo monthly budget`
- Amount: INR 1000/month
- Scope: `vocalingo-503812`
- Thresholds: 50%, 90%, 100%

## Security Follow-Ups

The APK currently contains a static backend bearer token via `BuildConfig.BACKEND_API_TOKEN`. That protects the backend from casual unauthenticated traffic, but it is not a strong production secret because APKs can be inspected. Before public launch, replace or supplement it with Firebase App Check or user-scoped auth that the backend verifies per request.
