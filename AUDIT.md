# Seraph code audit — 2026-07-27

## Fixed in v0.4.3-dev.1

- Added a manual updater in About.
- Restricts update metadata and APK downloads to HTTPS GitHub hosts.
- Requires a valid 64-character SHA-256 in the release manifest.
- Verifies the downloaded APK hash before installation.
- Verifies the APK package name is `com.typezero.seraph`.
- Verifies the update APK is signed by the same signer as the installed app.
- Uses a non-exported, cache-scoped `FileProvider` for installer handoff.
- Aligned development version metadata and removed the hard-coded About fallback version.
- Added an example update manifest and release notes entry.

## High priority remaining

1. **Incomplete Gradle wrapper in the supplied archive.** `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` are absent. The project cannot build from a clean checkout as documented until the official Gradle 8.9 wrapper files are restored.
2. **pCloud bearer token storage is plaintext.** `PCloudSession` stores the token in app-private `SharedPreferences`. Move it to Android Keystore-backed encryption before a broader release.
3. **Web login token extraction is heuristic.** The WebView scans API requests and localStorage. The resulting token is validated against pCloud before storage, which limits false positives, but this remains more fragile than a supported OAuth flow.
4. **Authentication tokens are placed in pCloud API query strings.** HTTPS protects transport, but query strings may be exposed to diagnostics or intermediary logging. Prefer request-body/header authentication if pCloud supports it for every endpoint used.
5. **No automated tests.** Rename collision handling, pCloud replacement rollback, filename sanitization, MusicBrainz parsing, and updater manifest validation need unit/instrumentation coverage.

## Medium priority remaining

- The two-pass batch rename does not restore files that reach a temporary name but fail during the final rename.
- pCloud replacement cleanup failures are intentionally ignored; an undeleted backup can remain after a successful replacement.
- Network code uses raw `HttpURLConnection` independently in multiple classes, with no shared cancellation, retry, or response-size policy.
- Recursive SAF and pCloud scans can be expensive for very large libraries and provide no progress or cancellation.
- `allowBackup="true"` should be reviewed alongside the token-storage design to ensure credentials are excluded from backup.
- The update manifest itself is not signed. Same-signer APK verification prevents installation of a foreign-signed APK, but a signed manifest would strengthen release metadata integrity and downgrade/availability guarantees.

## Release requirements for the updater

Publish `update-manifest.json` at the path configured in `UpdateManager.MANIFEST_URL`. Replace the placeholder hash with the lowercase SHA-256 of the signed APK. The APK must use the same signing key as the currently installed Seraph build.
