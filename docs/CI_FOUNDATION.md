# CI/CD Foundation Documentation

This repository establishes an automated CI/CD pipeline and lightweight Android build foundation.

## Build Environment & Versions Selected

* **Gradle**: 8.8
* **Android Gradle Plugin (AGP)**: 8.5.2
* **Kotlin**: 1.9.24
* **JDK Version**: 17
* **Compile SDK**: 34 (Android 14)
* **Target SDK**: 34 (Android 14)
* **Minimum SDK**: 27 (Android 8.1)
* **Build Variants**: `debug` (default), `release`

## Project Structure & Dependencies

The project foundation is designed to be lightweight and compatible with resource-constrained Android 8.1 devices.
* **Module**: `:app`
* **Package / Namespace**: `com.agent.android`
* **Dependencies**: `androidx.core:core-ktx:1.13.1`, `androidx.appcompat:appcompat:1.7.0`, `junit:junit:4.13.2`

## Gradle Commands

The following deterministic Gradle commands are used locally and in CI:

* **Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
* **Android Lint**:
  ```bash
  ./gradlew lintDebug
  ```
* **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
* **Full Verification Pipeline**:
  ```bash
  ./gradlew testDebugUnitTest lintDebug assembleDebug
  ```

## Generated APK Location & Artifacts

* **Local Generated APK Location**:
  `app/build/outputs/apk/debug/app-debug.apk`
* **Artifact Name in GitHub Actions**:
  `app-debug`

## GitHub Actions Workflow

The CI workflow is configured in `.github/workflows/android.yml`.

### Triggers
* Pushes to any branch (`push`)
* Pull requests against any branch (`pull_request`)

### Steps Executed in Workflow
1. **Checkout repository**: `actions/checkout@v4`
2. **Validate Gradle Wrapper**: `gradle/actions/wrapper-validation@v3`
3. **Set up JDK 17**: `actions/setup-java@v4` (Temurin JDK 17)
4. **Setup Gradle**: `gradle/actions/setup-gradle@v3`
5. **Run Unit Tests**: `./gradlew testDebugUnitTest`
6. **Run Android Lint**: `./gradlew lintDebug`
7. **Build Debug APK**: `./gradlew assembleDebug`
8. **Verify Debug APK Exists**: Asserts non-empty file at `app/build/outputs/apk/debug/app-debug.apk`
9. **Upload Debug APK Artifact**: `actions/upload-artifact@v4`

### CI Failure Conditions
The workflow will fail if:
* Gradle Wrapper checksum validation fails
* Kotlin compilation fails
* Any unit test fails
* Android Lint produces error level violations
* Debug APK fails to compile or assemble
* APK file is missing or has a zero byte size after build

## Downloading the APK Artifact from GitHub Actions

1. Go to the repository's **Actions** tab on GitHub.
2. Select the specific workflow run.
3. Scroll down to the **Artifacts** section at the bottom of the summary page.
4. Download the `app-debug` zip file containing `app-debug.apk`.

## Reproducing the Build Locally

1. Ensure JDK 17 is installed and configured on your machine.
2. Clone the repository and navigate to the project root directory.
3. Run the complete build and verification sequence:
   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```
4. Confirm that `app/build/outputs/apk/debug/app-debug.apk` exists and is non-empty.
