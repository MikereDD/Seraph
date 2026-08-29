# Security policy

## Supported versions

Seraph is currently under active development. Security fixes are applied to the
current development line and carried forward into later releases.

## Reporting a vulnerability

Please do **not** publish credentials, pCloud authentication tokens, private
library paths, crash logs containing personal information, or other sensitive
data in a public issue.

For a security-sensitive problem, use GitHub's private security-reporting tools
when available. If private reporting is not available, open a minimal issue that
contains no sensitive details and requests a private contact channel.

Useful reports include:

- affected Seraph version
- Android version and device model
- clear reproduction steps that do not expose private data
- expected and actual behavior
- whether the issue affects local files, pCloud files, the updater, or login

## Updater trust model

Seraph's updater is user initiated. Before handing an APK to Android's package
installer, it verifies the HTTPS source, SHA-256 checksum, package identity, and
signing-certificate continuity. Seraph does not silently install updates or
silently downgrade an installed build.
