<p align="center">
  <img src="docs/icon.png" width="132" alt="Seraph" />
</p>

<h1 align="center">Seraph</h1>

<p align="center">
  <strong>A premium music tag editor and library workflow for Android.</strong><br />
  Browse, identify, retag, add artwork, and safely rename music on-device or in pCloud.
</p>

<p align="center">
  <img alt="Android 8+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" />
  <img alt="License" src="https://img.shields.io/badge/license-Apache--2.0-blue" />
</p>

<p align="center">
  <a href="CHANGELOG.md">Changelog</a> ·
  <a href="docs/RELEASING.md">Release process</a> ·
  <a href="SECURITY.md">Security</a> ·
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

## What is Seraph?

Seraph is an Android music-library tool built around a simple idea: metadata
maintenance should be fast enough for one track and safe enough for an entire
album.

It can work with music selected through Android's Storage Access Framework or
with a pCloud library, read and write common audio tags, retrieve metadata from
MusicBrainz, fetch cover art through the Cover Art Archive, preview album-wide
changes before applying them, and optionally rename files using a predictable
template.

The current premium interface uses a graphite foundation with restrained teal
and violet accents across the Library, Tag Editor, and Album Match workflows.

## Core principles

- **Preview before destructive work** — album tagging and renaming expose the planned changes first.
- **Metadata and filenames are separate decisions** — album matching defaults to tag writes only; renaming is opt-in.
- **Verify important writes** — artwork and remote replacements are checked before success is reported.
- **Keep source choices explicit** — Device and pCloud remain distinct, visible library sources.
- **No password collection** — pCloud authentication occurs through pCloud's own web login; Seraph stores the resulting token, not the password.

## Features

### Library

- Device access through Android's Storage Access Framework with persisted read/write permission
- pCloud browsing through the pCloud HTTP API
- directory-first music browsing
- multi-select with folder-level batch actions
- premium folder and track cards with clear selected states

### Tag Editor

Seraph reads and writes metadata for formats supported by its jaudiotagger-based
workflow, including MP3, FLAC, M4A/MP4, Ogg, Opus, WAV, AAC, and WMA where the
underlying format permits the requested operation.

The editor supports:

- title, artist, album, album artist, track, disc, year, genre, and comments
- embedded front-cover artwork
- MusicBrainz lookup while keeping every field manually editable
- explicit loading, missing-artwork, unreadable-artwork, and valid-artwork states
- post-write artwork verification

### Album Match

Album Match searches MusicBrainz for a release, maps selected files to tracks,
and builds a dry-run plan before modifying anything.

- query derivation from existing metadata, parent folder, and filename clues
- recommended and alternate release results
- per-track matching plan
- planned tag-write and artwork-write counts
- optional file renaming
- live apply progress
- persistent completion summary with successes and failures

### Safe rename workflow

Per-folder rename operations use a token template such as:

```text
{track} of {total} - {artist} - {title}
```

Seraph shows the proposed names first and sanitizes them for common Android and
Windows filesystem restrictions.

### pCloud

pCloud authentication is performed on pCloud's own web login page, including
supported two-factor or federated-login flows. Seraph captures the authenticated
session token, validates the account region, and uses that token for API calls.

The password itself is not collected or stored by Seraph.

### Secure updater

Seraph includes a user-initiated sideload updater. Before Android is allowed to
install an update, Seraph verifies:

- HTTPS-only release URLs from approved hosts
- SHA-256 of the downloaded APK
- expected package name (`com.typezero.seraph`)
- signing-certificate match with the currently installed Seraph build

There are no silent updates and no automatic downgrades.

## Project status

**Current development source:** `0.4.3-dev.6` (`versionCode 51`)

The premium Library, Tag Editor, and Album Match passes are implemented. Recent
work corrected album-query derivation, improved artwork diagnostics and
verification, and added visible apply progress plus persistent completion
results. The project is now being prepared for standalone releases and continued
real-device testing.

## Releases

Release APKs are intentionally **not stored in Git**. Signed builds belong in the
matching GitHub Release as release assets, while the updater manifest remains in
source control.

This README intentionally contains no direct APK download link. See
[`docs/RELEASING.md`](docs/RELEASING.md) for the release workflow.

## Build from source

### Requirements

- JDK 17
- Android SDK 35
- Gradle 8.9
- Android Gradle Plugin 8.7.2
- Kotlin 2.0.21

With Gradle 8.9 installed locally:

```bash
gradle :app:assembleDebug
```

A local wrapper can also be generated if desired:

```bash
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
```

The generated Gradle wrapper binaries are not required to live in this
repository.

### Toolchain

| Component | Version |
|---|---|
| Gradle | 8.9 |
| Android Gradle Plugin | 8.7.2 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.01 |
| jaudiotagger Android fork | 2.3.15 |
| min / target SDK | 26 / 35 |
| JDK | 17 |

## Project layout

```text
app/src/main/java/com/typezero/seraph/
├─ SeraphApp.kt
├─ MainActivity.kt
├─ di/                       # manual application wiring
├─ data/
│  ├─ model/                 # audio and metadata models
│  ├─ tagging/               # tag reads/writes + verification
│  ├─ musicbrainz/           # MusicBrainz + Cover Art Archive
│  └─ rename/                # filename templates + safe rename planning
├─ storage/                  # storage abstraction + SAF implementation
├─ pcloud/                   # pCloud session/client/storage implementation
└─ ui/
   ├─ library/
   ├─ editor/
   ├─ albummatch/
   ├─ rename/
   ├─ login/
   ├─ about/
   ├─ components/
   └─ theme/
```

## Storage notes

jaudiotagger requires a real local file. Seraph therefore stages metadata edits
through an app cache file before writing the result back to SAF or pCloud.
Remote replacements are verified before being reported as successful.

The pCloud authentication token is application-private data and should never be
included in bug reports, screenshots, logs, backups shared with others, or
repository files.

## Release and update model

The production updater manifest lives at:

```text
releases/update.json
```

The example schema is in [`releases/update.example.json`](releases/update.example.json).
A real release manifest points at a signed APK attached to the corresponding
GitHub Release and records that APK's SHA-256. Release APK binaries themselves
remain outside Git history.

See [`docs/RELEASING.md`](docs/RELEASING.md) for the complete release workflow.

## Repository history

Seraph began inside the `It-Works-On-My-Machine` monorepository. Its
application-specific history was extracted into this standalone repository with
`git filter-repo`, preserving relevant development history while removing
unrelated monorepo tags and historical APK binaries.

See [`docs/MONOREPO-MIGRATION.md`](docs/MONOREPO-MIGRATION.md).

## Contributing and security

Seraph is primarily a personal project, but focused bug reports and sensible
pull requests are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

For vulnerabilities or security-sensitive reports, follow [`SECURITY.md`](SECURITY.md).
Never include pCloud tokens, passwords, or private library information in a
public report.

## License

Seraph's original source is licensed under the **Apache License 2.0**.
Third-party components, services, metadata, and artwork retain their own
licenses, notices, and rights.

See [`LICENSE`](LICENSE), [`NOTICE`](NOTICE), and
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

---

<p align="center">
  <strong>Seraph</strong><br />
  Built by Typezer∅
</p>
