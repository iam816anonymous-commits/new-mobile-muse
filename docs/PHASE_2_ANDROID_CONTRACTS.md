# Phase 2 Android Platform Contracts

## Utilized Android Platform Contracts

| Skill | Android System Contract / API | Extras / Parameters |
| :--- | :--- | :--- |
| **Set Timer** | `AlarmClock.ACTION_SET_TIMER` | `EXTRA_LENGTH` (seconds), `EXTRA_MESSAGE`, `EXTRA_SKIP_UI` |
| **Set Alarm** | `AlarmClock.ACTION_SET_ALARM` | `EXTRA_HOUR`, `EXTRA_MINUTES`, `EXTRA_MESSAGE`, `EXTRA_SKIP_UI` |
| **Web Search** | `Intent.ACTION_WEB_SEARCH` | `SearchManager.QUERY` ("query") |

## Non-Interference Policy

LocalAgent fires platform Intents with `Intent.FLAG_ACTIVITY_NEW_TASK` and does NOT crawl or automate the launched Clock or Browser UI.
