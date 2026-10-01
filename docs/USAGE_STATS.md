# LocalAgent Usage Stats & Foreground App Subsystem

## Overview
`UsageStatsController` inspects recent application usage via `UsageStatsManager` to determine active foreground applications.

## Commands
- `app current`: Queries active foreground package name.
- `app list`: Lists launchable applications.
- `app find <query>`: Resolves installed app package by name.
- `app info <package>`: Queries package version and metadata.

## Special Access
- Requires `USAGE_STATS_ACCESS` (App Ops `OPSTR_GET_USAGE_STATS`). Returns `USAGE_STATS_ACCESS_REQUIRED` when ungranted.
