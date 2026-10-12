# Build AudioOnly APK with GitHub Actions

This workflow intentionally uses a provisioned Gradle 8.9 installation rather than `./gradlew`, because this source archive does not contain the Gradle Wrapper scripts or wrapper JAR.

## Install in an existing GitHub repository

Copy `.github/workflows/build-apk.yml` into the repository's `.github/workflows/` folder. The workflow supports either of these layouts:

1. Android project files (`settings.gradle.kts`, `build.gradle.kts`, `app/`) at the repository root; or
2. Those files inside `AudioOnlyBlackScreen/`.

Commit and push the workflow. In GitHub, open **Actions → Build AudioOnly APK → Run workflow**. After success, download the `AudioOnly-debug-APK` artifact.

The workflow installs Android platform 35 and build-tools 35.0.0 and builds the debug APK. A successful build still needs to be tested on real phones and TVs.
