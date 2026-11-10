# Spec: Mars Rover (Open Day Edition)

Source: `docs/REQUIREMENTS.md`, narrowed by the scope interview on 2026-11-10.
Deadline: **open day 2026-11-13**. Built by Michael + Claude. Stability beats features.

## 1. Objective

A JavaFX kiosk game. Visitors (13–14 y) build Java programs from drag & drop blocks
(Parsons puzzle) to steer a Mars rover, then watch the program run line by line.

**Session flow:** start screen (leaderboard + name entry) → levels 1–4 → end screen
(score, leaderboard) → start screen. 60 s inactivity anywhere → back to start screen.

### In scope
- Name entry (max 12 chars, word filter)
- Parsons editor: palette left, program right, mouse drag & drop, auto-indent by nesting,
  distractor blocks, editable numbers (click / scroll), optional `maxBlocks`
- Blocks: `vor`, `linksUm`, `rechtsUm`, `probeNehmen`, `for`, `while`, `if`, `if/else`
- Run / Step / Reset, 3-speed slider, current-line highlight
- Friendly error messages + Java line hint, success animation, stars, "Next level"
- Levels 1–4 as JSON in `levels/`, reloadable at runtime
- Full-screen kiosk, 60 s auto-reset, admin shortcut + PIN (exit / reload levels / clear leaderboard)
- Top-10 leaderboard in the same window, stored as JSON
- Unit tests for interpreter, level loader, scoring, leaderboard store

### Out of scope
Pixelwand, expert (text) mode, second screen, everything in REQUIREMENTS §12,
network, accounts, persistence across days.
**Only if time remains:** methods / level 5, "fix the bug" levels, Undo (`Ctrl+Z`).

## 2. Tech Stack
- JDK 25 (`maven.compiler.release=25`; dev machine has JDK 27, fine)
- JavaFX 25 (`org.openjfx:javafx-controls`), plain code UI, no FXML
- Jackson Databind for JSON
- JUnit 5 for tests
- Maven 3.9, `javafx-maven-plugin` for dev runs, `maven-shade-plugin` for one runnable jar

## 3. Commands
```
Test:    mvn -q test
Run dev: mvn -q javafx:run
Package: mvn -q package            → target/mars-rover.jar
Kiosk:   java -jar mars-rover.jar  (run from a folder containing levels/ and data/)
```

## 4. Project Structure
```
pom.xml
levels/                         → level JSON files (01-landing.json …), loaded in filename order
data/leaderboard.json           → created at runtime
data/config.properties          → adminPin, inactivitySeconds (defaults: 1234, 60)
data/badwords.txt               → one lowercase word per line
src/main/java/marsrover/
  App.java, Launcher.java       → Launcher.main is the shaded jar entry (doesn't extend Application)
  model/                        → Tile, Direction, World, Rover, Level, LevelLoader, Stars
  program/                      → Block AST, Interpreter, StepResult, RunError
  session/                      → Session, Leaderboard, NameFilter, Config
  view/                         → screens, EditorPane, WorldCanvas, AdminDialog
src/main/resources/levels/      → bundled copies of default levels (fallback if levels/ missing)
src/test/java/marsrover/        → mirrors main packages
```
`model` and `program` must not import JavaFX. `view` depends on them, never the reverse.

## 5. Game Rules (decisions that close gaps in REQUIREMENTS)

### Tiles
One char per tile, no spaces, each row exactly `width` long:
`G` ground · `R` rock · `C` crater · `S` sample (on ground) · `B` base · `D` dust.
(The spaced row in REQUIREMENTS §10 is a typo.) Exactly one `B` per level.

### Commands
| Command | Battery | Failure |
|---|---|---|
| `vor()` | 1 (2 if target is `D`) | rock or grid edge → "Der Rover ist gegen einen Felsen gefahren!"; crater → "Der Rover ist in einen Krater gefallen!" |
| `linksUm()` / `rechtsUm()` | 1 | — |
| `probeNehmen()` | 1 | no sample → "Hier liegt keine Probe!" |
| sensors | 0 | — |

- A command needing more battery than is left → "Batterie leer!"
- Collected `S` becomes `G`.
- **Win check at program end:** all samples collected and rover on `B`. Otherwise:
  "Mission nicht geschafft: noch N Proben" / "… der Rover ist nicht an der Basis".
- **Endless-loop guard:** 1000 interpreter steps (each action and each condition check counts)
  → "Der Rover dreht sich im Kreis – Endlosschleife?"
- Every error shows a Java hint: `Zeile 4: vor();`

### Blocks and palette
Blocks are **fixed Parsons lines**: a condition is part of the block, not a slot to fill.
The palette entries in a level file are strings:
```
"vor" "linksUm" "rechtsUm" "probeNehmen"
"for:3"                 → for (int i = 0; i < 3; i++) {   (number editable)
"while:vornFrei"        → while (vornFrei()) {
"while:!amZiel"         → while (!amZiel()) {
"if:vornKrater"         → if (vornKrater()) {
"ifelse:vornKrater"     → if (vornKrater()) { … } else { … }
```
- The palette is infinite: dragging out makes a copy. Listing an entry twice has no effect.
- Distractors are just extra palette entries.
- `for` counts are editable 1–20: left click +1, right click −1, scroll ±1.
  (Right-click deletes only on a block's body, not its number.)
- `maxBlocks`: a drop that would exceed it is refused, and the counter flashes.

### Stars and scoring
- ★ solved · ★★ plus battery used ≤ `stars.battery` · ★★★ plus block count ≤ `stars.blocks`.
- Block count = number of blocks in the program. A control block counts 1, its body counts separately.
- Per level, the best stars of that session count.
- **Score** = total stars over levels 1–4 (max 12). **Tiebreak** = total session time, lower wins.
- A session result goes on the leaderboard if ≥1 level was solved. This happens at the end screen
  or on auto-reset.

### Execution
- The interpreter is a pure, steppable machine: `step()` executes until the next **action**
  and returns `StepResult(line, worldSnapshot, status)`. No threads in `program`.
- Run = a JavaFX `Timeline` calling `step()` every 600 / 300 / 100 ms (slow/normal/fast).
- Step (`F6`) = one `step()`. The editor is locked while a run is in progress.
- Reset (`R`) restores world and rover and keeps the program.

## 6. Level Format
```json
{
  "id": "02-long-road",
  "title": "Lange Straße",
  "intro": "Die Basis ist weit weg. Willst du wirklich 9× vor() ziehen?",
  "width": 10, "height": 3,
  "tiles": ["RRRRRRRRRR", "GGGGGGGGGB", "RRRRRRRRRR"],
  "rover": { "x": 0, "y": 1, "dir": "E" },
  "battery": 20,
  "palette": ["vor", "linksUm", "for:5", "probeNehmen"],
  "maxBlocks": 4,
  "stars": { "battery": 12, "blocks": 2 },
  "program": null
}
```
`maxBlocks` and `program` are optional (`program` is reserved for fix-the-bug levels).
The loader validates the level and reports **file + reason** on invalid input. It checks:
row count and lengths, unknown chars, one base, rover on a passable tile, unknown palette entries.
Invalid files are skipped and shown in the admin dialog. They never crash the app.

## 7. Kiosk, Session, Admin
- Full screen, `fullScreenExitKeyCombination = NO_MATCH`, window close requests are consumed
  (only admin "Exit" quits).
- Name: 1–12 chars from letters, digits and space, trimmed. Rejected if any badword is a
  substring (case-insensitive) → "Bitte wähle einen anderen Namen."
- Inactivity: any mouse or key event resets a 60 s timer. On timeout the session result is
  saved (see 5) and the start screen shows. A running program counts as activity.
- `Ctrl+Shift+A` → PIN prompt → Reload levels / Clear leaderboard / Exit / Cancel.
- Shortcuts: `F5`/`Enter` Run · `F6` Step · `R` Reset · `Del` delete selected block ·
  right-click deletes a block.

## 8. Code Style
Records and sealed interfaces for the AST, no JavaFX in the core, small classes.
```java
package marsrover.program;

public sealed interface Block permits Command, Repeat, While, If {}

public record Command(Action action) implements Block {}
public record Repeat(int count, List<Block> body) implements Block {}
public record While(Condition cond, List<Block> body) implements Block {}
public record If(Condition cond, List<Block> then, List<Block> otherwise) implements Block {}

public record Condition(Sensor sensor, boolean negated) {}
```
- Identifiers in English. The visitor-facing Java API keeps its German names (`vor`, `linksUm` …).
- UI texts in German, kept in one `Texts` class.
- No dependencies beyond the stack above without asking.

## 9. Testing Strategy
- JUnit 5, `src/test/java` mirroring the packages.
- **Interpreter:** each command and its battery cost, each failure message, for/while/if/else
  nesting, the step guard, win/lose at program end, line numbers in `StepResult`.
- **LevelLoader:** the four default levels load. Every validation rule rejects bad input with a message.
- **Stars/score, NameFilter, Leaderboard** (top-10 ordering, tiebreak, file round-trip).
- **Default levels are solvable:** a test runs a reference solution per level and gets 3 ★.
- **UI:** manual checklist in `docs/TESTPLAN.md`, run on a 1920×1080 screen before the open day.

## 10. Boundaries
- **Always:** run `mvn -q test` before each commit. Keep `model`/`program` JavaFX-free. Bad level
  files and bad leaderboard files must never crash the app.
- **Ask first:** new dependencies, anything from "only if time remains", changes to scoring rules.
- **Never:** compile or run real Java from visitor input. Commit to `main` without green tests.
  Add out-of-scope features.

## 11. Success Criteria
1. `mvn -q test` passes. Default levels 1–4 are each proven solvable with 3 ★.
2. A first-time tester solves level 1 in under 1 minute with no explanation.
3. A full session (levels 1–4) takes 3–5 minutes for a tester with no prior knowledge.
4. 30 minutes of random clicking/dragging/keys on the target PC: no crash, no hang,
   auto-reset works, no way out of full screen without the PIN.
5. A student copies a new JSON into `levels/`, uses admin → Reload, and the level appears.
   A broken JSON shows an error in the admin dialog instead of crashing.
6. The leaderboard survives an app restart. Admin can clear it.

## 12. Decisions (resolved 2026-11-10)
1. **UI language:** German.
2. **Kiosk PC OS:** Windows. The shaded jar bundles the Windows JavaFX natives (`win` classifier) plus
   the dev machine's natives (`mac-aarch64`), and Alt+F4 is consumed.
3. **No skip button.** Stuck visitors are handled by the 60 s auto-reset.
4. **Right-click on a `for` number** lowers it. Right-click anywhere else on a block deletes the block.
5. **Admin PIN:** default `1234` in `data/config.properties`. Change it before the day.
