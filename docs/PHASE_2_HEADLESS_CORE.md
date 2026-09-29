# Phase 2 Headless Core Skills

## Skills Summary

1. **Calculator Skill (`CalculatorSkill.kt`)**:
   In-memory Shunting-Yard (RPN) expression evaluator.
   * Operations: `+`, `-`, `*`, `/`, `%`, `^`, parentheses, decimals.
   * Error Handling: Division by zero (`DIVISION_BY_ZERO`), malformed parentheses (`INVALID_EXPRESSION`).

2. **Notes Skill (`NotesSkill.kt`)**:
   Appends `[yyyy-MM-dd HH:mm] content` lines to `Download/LocalAgent/notes.txt`.
   * Error Handling: Returns `PERMISSION_REQUIRED` if runtime `WRITE_EXTERNAL_STORAGE` is denied on API 27.

3. **Intent Skills (`IntentSkills.kt`)**:
   Uses platform Intents without UI crawling.
   * **Timer**: `AlarmClock.ACTION_SET_TIMER` with duration validation.
   * **Alarm**: `AlarmClock.ACTION_SET_ALARM` with hour/minute bounds check.
   * **Web Search**: `Intent.ACTION_WEB_SEARCH` with empty query rejection.
