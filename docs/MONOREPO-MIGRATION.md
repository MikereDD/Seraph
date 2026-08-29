# Repository migration

Seraph began inside the `It-Works-On-My-Machine` monorepository. Its
application-specific history was extracted with `git filter-repo` and moved into
a standalone repository rather than restarting the project with a synthetic
initial commit.

Unrelated monorepo tags and historical APK binaries were intentionally removed
from the standalone history. Release APKs belong in release assets, not Git
history.
