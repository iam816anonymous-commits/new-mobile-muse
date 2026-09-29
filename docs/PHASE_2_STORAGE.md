# Phase 2 Persistent Storage & Notes Strategy

## User Notes vs Long-Term Agent Learning

* **User Notes**: Persisted in user-visible shared Downloads directory (`Download/LocalAgent/notes.txt`). Uses direct stream appending to prevent holding notes in RAM.
* **Agent Learning Store**: Managed separately via SAF (`LearningStore.kt`).

## API 27 Permissions & Safeguards

* Writing to `Environment.DIRECTORY_DOWNLOADS` on Android 8.1 requires `WRITE_EXTERNAL_STORAGE`.
* If permission is missing, `NotesSkill` returns `SkillStatus.PERMISSION_REQUIRED` without crashing or hanging.
