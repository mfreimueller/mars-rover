# Tasks – Mars Rover

Verify each task with `mvn -q test` unless noted. Commit after each green task.

## Phase 1 – Core

- [x] **T1: Project skeleton + runnable jar** (S)
  - Acceptance: `pom.xml` (release 25, JavaFX 25, Jackson, JUnit 5, javafx + shade plugins with `win` and `mac-aarch64` natives); `Launcher` opens an empty full-screen window; `.gitignore`.
  - Verify: `mvn -q package && java -jar target/mars-rover.jar` shows the window; one dummy test passes.
  - Files: `pom.xml`, `.gitignore`, `App.java`, `Launcher.java`, one test

- [x] **T2: Model + LevelLoader** (M) – depends on T1
  - Acceptance: `Tile`, `Direction`, `World`, `Rover`, `Level` records; `LevelLoader` loads a folder in filename order and returns valid levels plus `(file, reason)` errors for every rule in SPEC §6.
  - Verify: tests for one valid level and each validation rule.
  - Files: `model/*`, `LevelLoaderTest`

- [x] **T3: Interpreter** (M) – depends on T2
  - Acceptance: sealed `Block` AST; palette-entry parser (`"for:3"`, `"while:!amZiel"`); steppable `Interpreter.step()` → `StepResult(line, world, status)`; all rules from SPEC §5 (costs, dust, failures + German messages, 1000-step guard, win check at end).
  - Verify: `InterpreterTest` covers each command, each failure, nesting, the guard, win/lose and line numbers.
  - Files: `program/*`, `InterpreterTest`, `PaletteEntryTest`

- [x] **T4: Default levels 1–4 + Stars** (S) – depends on T3
  - Acceptance: `levels/01-landing.json` … `04-unknown-distance.json` (packed into the jar by Maven); `Stars` calculation; a reference solution per level.
  - Verify: `DefaultLevelsTest` runs each reference solution and asserts 3★.
  - Files: `levels/*.json`, `model/Stars.java`, `DefaultLevelsTest`

### ✅ Checkpoint A – tests green, jar runs on Mac + kiosk PC

## Phase 2 – Playable level

- [x] **T5: Level screen with execution** (M) – depends on T4
  - Acceptance: `WorldCanvas` draws tiles, rover direction and battery. Code pane shows a hard-coded program with line numbers. Run/Step/Reset and 3-speed slider animate it and highlight the current line.
  - Verify: manual – level 2 runs with its reference solution at all speeds; Reset restores the start.
  - Files: `view/LevelScreen`, `view/WorldCanvas`, `view/CodeView`, `view/Texts`

- [x] **T6: Editor – drag & drop** (M) – depends on T5
  - Acceptance: palette from the level; dragging copies a block into the program; drop gap and depth chosen by mouse position, with a visible drop indicator; blocks can be moved within the program; auto-indented valid Java; rows ≥ 40 px; editor → AST for Run.
  - Verify: `EditorModelTest` (insert/move/nesting → expected AST + lines); manual drag test.
  - Files: `view/EditorPane`, `program/EditorModel` (pure), `EditorModelTest`

- [x] **T7: Editor – delete, numbers, maxBlocks** (S) – depends on T6
  - Acceptance: right-click or `Del` on a selected block deletes it (with its body); `for` count 1–20 changes on left click, right click and scroll; block counter shown; drop refused at `maxBlocks` with the counter flashing; editor locked while running.
  - Verify: `EditorModelTest` additions; manual check.
  - Files: `view/EditorPane`, `program/EditorModel`

- [x] **T8: Feedback + stars** (S) – depends on T7
  - Acceptance: error banner with friendly text + `Zeile N: …` hint; success overlay with stars and "Nächstes Level".
  - Verify: manual – trigger crater, rock, battery, endless loop and incomplete mission; solve with 1, 2 and 3 stars.
  - Files: `view/LevelScreen`, `view/ResultOverlay`
  - Added after the first playtest: a goal line under the intro ("Ziel: …"), a tile legend under the map and a sample counter ("Proben: 0 / 1"). Level 3 has no sample, and probeNehmen there is only a distractor, which confused the tester.

### ✅ Checkpoint B – levels 1–4 playable by mouse; hallway test with a newcomer

## Phase 3 – Kiosk product

- [ ] **T9: Session flow + name entry** (M) – depends on T8
  - Acceptance: start screen → name (NameFilter, `badwords.txt`) → levels 1–4 → end screen (stars, time) → start; best stars per level count.
  - Verify: `NameFilterTest`, `SessionTest` (score, tiebreak time); manual full session.
  - Files: `session/Session`, `session/NameFilter`, `view/StartScreen`, `view/EndScreen`

- [ ] **T10: Leaderboard** (S) – depends on T9
  - Acceptance: `data/leaderboard.json` holds the top 10 (stars desc, time asc), shown on the start and end screens; a corrupt or missing file means an empty board, never a crash.
  - Verify: `LeaderboardTest` (ordering, cut at 10, round-trip, corrupt file).
  - Files: `session/Leaderboard`, `LeaderboardTest`, start/end screens

- [ ] **T11: Kiosk lock, shortcuts, inactivity** (S) – depends on T9
  - Acceptance: full screen with no exit combo; close requests consumed; F5/Enter/F6/R/Del work; 60 s inactivity saves the session and returns to start; an uncaught exception returns to start.
  - Verify: manual – Esc, Alt+F4 and Cmd+Q don't exit; idle 60 s resets.
  - Files: `App`, `session/Config`, `view/*` key handling

- [ ] **T12: Admin dialog** (S) – depends on T10, T11
  - Acceptance: Ctrl+Shift+A → PIN (from `data/config.properties`) → reload levels (lists invalid files), clear leaderboard, exit, cancel.
  - Verify: manual – drop in a new JSON and reload; drop in broken JSON and see the error; wrong PIN does nothing.
  - Files: `view/AdminDialog`, `session/Config`

### ✅ Checkpoint C – full session, restart-safe leaderboard, live level reload

## Phase 4 – Ship

- [ ] **T13: Package + rehearsal** (S)
  - Acceptance: a distributable folder (`mars-rover.jar`, `levels/`, `data/`, `start.bat`); `docs/TESTPLAN.md` (manual checklist); `docs/LEVELS.md` (how students write levels).
  - Verify: SPEC §11 success criteria 1–6 on the kiosk PC, including 30 min of random abuse.

## Stretch (ask first)
- [ ] Undo (`Ctrl+Z`) · [ ] Fix-the-bug levels (`program` field) · [ ] Methods / level 5
