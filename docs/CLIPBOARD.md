# LocalAgent Clipboard Subsystem

## Overview
`ClipboardController` manages on-demand system clipboard access using Android `ClipboardManager`.

## Commands
- `clipboard status`: Queries primary clip existence and length.
- `clipboard read`: Reads current primary clip text.
- `clipboard write <text>`: Writes text string to clipboard.
- `clipboard clear`: Clears primary clip contents.

## Safety & Resource Rules
- On-demand execution only; no continuous clipboard polling or background monitoring.
- Handles empty or null clip data safely without crashing.
