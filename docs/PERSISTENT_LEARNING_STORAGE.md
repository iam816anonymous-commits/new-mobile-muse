# Persistent Learning Storage Strategy

## Storage Architecture

Persistent learning data cannot rely exclusively on app-private directories (`filesDir`, `cacheDir`, `getExternalFilesDir()`) because those directories are deleted upon app uninstallation.

### Persistence Strategy

* **Storage Access Framework (SAF)**:
  Uses SAF user-selected documents/directories (`Intent.ACTION_OPEN_DOCUMENT_TREE` / `ACTION_CREATE_DOCUMENT`) for user-owned shared storage.
* **App Update vs. Uninstall/Reinstall**:
  * **App Updates**: Data and URI access permissions persist across standard updates.
  * **App Uninstall / Reinstall**: The underlying files in shared/user-selected storage persist on disk, but app-held URI permission grants are invalidated by Android OS. Upon reinstall, the onboarding flow asks the user to re-select or import their learning directory (`LearningStore.importData()`).
* **Permissions**:
  `MANAGE_EXTERNAL_STORAGE` is NOT requested.
* **Integrity & Versioning**:
  All records include `schemaVersion` and checksum metadata. Schema versions higher than supported are rejected during parsing.
