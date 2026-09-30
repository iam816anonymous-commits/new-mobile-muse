# LocalAgent Permission Audit & Matrix

## Permission Classification

| Permission / Access | Type | Required By | Status |
|---|---|---|---|
| `RECORD_AUDIO` | Runtime Permission | Speech-to-Text (`stt.listen`) | Declarative & Guided |
| `CAMERA` | Runtime Permission | Flashlight Torch & Camera Info | Declarative & Guided |
| `WRITE_EXTERNAL_STORAGE` | Runtime Permission | Notes persistent storage | Declarative & Guided |
| `WRITE_SETTINGS` | Special Access | Screen brightness & Screen timeout | Guided Intent |
| Notification Policy Access | Special Access | Silent ringer mode | Guided Intent |
| Notification Listener Access | Special Access | Notification observation | Guided Intent |
| Usage Stats Access | Special Access | Foreground app detection | Guided Intent |
| Accessibility Service | Special Access | Panic gesture & Navigation | User Enabled |
