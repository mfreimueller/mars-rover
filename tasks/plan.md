# Implementation Plan: Mars Rover (Open Day Edition)

Spec: `docs/SPEC.md`. Open day: **Fri 2026-11-13**. Build days: Tue 10 (evening), Wed 11, Thu 12.

## Overview
The rules engine comes first: world, level loader and interpreter, all pure Java with unit tests.
Then a vertical slice that runs a program on screen. Then the drag & drop editor, the riskiest UI part.
Session, leaderboard, kiosk and admin come last, because they are mostly wiring.
Every evening the app on `main` is runnable and the tests are green.

## Architecture Decisions
- **Steppable interpreter without threads.** The AST is compiled to a cursor/stack machine.
  `step()` runs until the next action. JavaFX drives it with a `Timeline`. This makes Run, Step,
  speed changes and stopping trivial and safe.
- **Fixed Parsons blocks.** Conditions are part of the block (`while:vornFrei`), so the editor
  only has to handle *placing* blocks, not filling slots. This halves the editor work.
- **Editor model is the AST itself.** The editor holds a mutable tree of block nodes and renders it
  as indented rows, one row per code line (closing `}` rows included). Line numbers come from that
  rendering and are shared with the interpreter for highlighting.
- **Drop targets are gaps between rows.** A gap at indent level k inserts into the matching body.
  Only one hit-test is needed: mouse y → gap, mouse x → nesting depth.
- **Shaded jar with `win` + `mac-aarch64` JavaFX natives.** It runs with `java -jar` on the kiosk's JDK 25.

## Phases

### Phase 1 – Core (Tue 10 / Wed 11 morning)
T1 Project skeleton + runnable jar · T2 Model + LevelLoader · T3 Interpreter · T4 Default levels

**Checkpoint A:** `mvn -q test` green, levels 1–4 proven 3★-solvable, jar opens a window.
**You:** test the jar on the kiosk PC today if possible.

### Phase 2 – Playable level (Wed 11)
T5 Level screen with run/step animation · T6 Editor: drag & drop · T7 Editor: delete, numbers, maxBlocks · T8 Feedback + stars

**Checkpoint B:** levels 1–4 are playable end to end with the mouse. Hallway test with one person
who hasn't seen the game.

### Phase 3 – Kiosk product (Thu 12)
T9 Session flow + name entry · T10 Leaderboard · T11 Kiosk lock + shortcuts + inactivity · T12 Admin dialog

**Checkpoint C:** a full session works. Leaderboard survives a restart. Admin reload picks up a new level.

### Phase 4 – Ship (Thu 12 evening)
T13 Package, TESTPLAN, level guide for students, rehearsal on the kiosk PC.

**Stretch (only with your OK):** Undo, fix-the-bug levels, methods/level 5.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|---|---|---|
| Drag & drop editor takes longer than a day | High | Simple gap-based drop model (see above). T6 is first on Wed. Fallback: click a palette block to append it at the selected position. |
| JavaFX natives / full screen behave differently on Windows | High | Build the jar in T1 and you try it on the kiosk PC the same day. |
| Kids find level 1 confusing | Med | Hallway test at Checkpoint B. Tune the intro texts and palette, not the code. |
| Endless loop or exception freezes the kiosk | High | Step guard in the interpreter. Global uncaught-exception handler returns to the start screen. |
| Broken JSON from students on the day | Med | Loader never throws to the UI. The admin dialog lists bad files. |
