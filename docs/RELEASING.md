# Seraph release process

Seraph release APKs are distributed as **GitHub Release assets**. APK binaries
are not stored in Git history.

## Release checklist

1. Update `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Update `CHANGELOG.md` and the project-status section of `README.md`.
3. Build the signed release APK with the canonical Seraph signing key.
4. Verify the APK package is `com.typezero.seraph` and verify its signing certificate.
5. Compute the APK SHA-256.
6. Create the matching GitHub Release and attach the signed APK.
7. Update `releases/update.json` with the release version, release-asset URL,
   SHA-256, and concise release notes.
8. Commit the manifest change only after the release asset exists.
9. Test the in-app manual updater from an older signed build.

Never commit signing keys, passwords, pCloud tokens, or release APK binaries to
the repository.
