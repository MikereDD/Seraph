# Contributing to Seraph

Seraph is primarily a personal project, but focused bug reports and sensible
pull requests are welcome.

## Before opening a change

- keep changes scoped to Seraph
- preserve the local-device and pCloud workflows
- do not commit APK/AAB binaries, credentials, tokens, signing keys, or local SDK paths
- keep destructive metadata and rename operations previewable and verifiable
- retain the established premium graphite / teal / violet interface language
- update `CHANGELOG.md` when behavior changes

## Building

Seraph uses JDK 17, Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.2,
and Kotlin 2.0.21.

The Gradle wrapper binaries are intentionally not required in the repository.
With Gradle 8.9 installed locally:

```bash
gradle :app:assembleDebug
```

## Security reports

Do not place secrets or private account data in issues. See `SECURITY.md` for
security-sensitive reporting guidance.
