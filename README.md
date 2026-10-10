# Lobby Parkour

A simple and customizable plugin that adds Parkour to your Minecraft Server

[Download on Modrinth!](https://modrinth.com/plugin/lobby-parkour)

## Showcase
[Watch on YouTube](https://youtu.be/ixqN2ViTfB8)

## Features
- Start, End and Checkpoints
- Leaderboards
- Customize each plate type
- Customize parkour messages
- Relocate Checkpoints
- Rename Parkour
- Place up to 26 parkours per server


## PlaceholderAPI
Requires [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi). All placeholders return plain text.

Parkour names are case-insensitive, and you can write `_` in place of spaces
(e.g. `New Parkour` → `%lobbyparkour_pk_new_parkour_top1%`).

### Parkour placeholders
`%lobbyparkour_pk_<name>_<stat>%`

| Stat | Description |
|---|---|
| `top<N>` | Leaderboard position N, formatted with `placeholders.top-format` (e.g. `Steve - 00:42:13`) |
| `top<N>_name` | Player name at position N |
| `top<N>_time` | Time at position N (uses the `timer` format) |
| `top<N>_seconds` | Time at position N in seconds (e.g. `42.13`) |
| `completions` | Total number of completed runs |
| `players` | Number of players who have completed the parkour |
| `best` | The viewing player's personal best |
| `best_seconds` | The viewing player's personal best in seconds |
| `position` | The viewing player's leaderboard position |
| `runs` | How often the viewing player has completed the parkour |

Example: `%lobbyparkour_pk_Spawn_top1%`, `%lobbyparkour_pk_Spawn_top3_name%`, `%lobbyparkour_pk_Spawn_best%`

### Player placeholders
| Placeholder | Description |
|---|---|
| `%lobbyparkour_in_parkour%` | `true` if the player is currently in a parkour |
| `%lobbyparkour_current_parkour%` | Name of the parkour the player is currently doing |
| `%lobbyparkour_current_time%` | Current run time |
| `%lobbyparkour_current_seconds%` | Current run time in seconds |
| `%lobbyparkour_current_checkpoint%` | Last reached checkpoint |
| `%lobbyparkour_current_checkpoint_total%` | Total checkpoints of the current parkour |
| `%lobbyparkour_completions%` | Total runs completed across all parkours |
| `%lobbyparkour_parkour_count%` | Number of parkours on the server |

Empty values (e.g. an unfilled leaderboard slot) can be configured under `placeholders` in the `config.yml`.
