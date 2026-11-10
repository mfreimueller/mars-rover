# Mars Rover – Requirements

Puzzle game for the Tag der offenen Tür (HTL, IT department), loosely based on the Java-Hamster model.
Visitors (approx. 13–14 years old) guide a Mars rover by arranging Java code blocks (Parsons puzzle).

## 1. Goals

- A visitor solves a first level within **1 minute**, with no explanation needed.
- One session (several levels) lasts **3–5 minutes**.
- Visitors see **real Java code** and its effect side by side.
- HTL students can add levels and commands **live during the day**.

## 2. Setting & Game World

- The playing field is a grid on the Martian surface.
- Tile types:
  - `GROUND`: passable
  - `ROCK`: blocked
  - `CRATER`: rover falls in and the level fails
  - `SAMPLE`: can be collected
  - `BASE`: goal tile
  - Optional: `DUST`, a passable tile that costs double battery
- The rover has a position, a direction (N/E/S/W) and a **battery**: each executed command costs energy.
- A level is won when all samples are collected and the rover reaches the base. Battery and step limits must be respected.

## 3. Commands (Rover API)

| Command | Type | Effect |
|---|---|---|
| `vor();` | action | move one tile forward |
| `linksUm();` | action | turn 90° left |
| `rechtsUm();` | action | turn 90° right (unlocked from level 2) |
| `probeNehmen();` | action | collect sample on current tile |
| `vornFrei()` | sensor | true if the next tile is passable |
| `vornKrater()` | sensor | true if the next tile is a crater |
| `probeDa()` | sensor | true if a sample is on the current tile |
| `amZiel()` | sensor | true if the rover is on the base |

Control structures as blocks: `for (int i = 0; i < N; i++) { }`, `while (…) { }`, `if (…) { } else { }`, and later methods (`void name() { }`).

## 4. Puzzle Mechanics

- **Parsons puzzle:** a block palette on the left and the program area on the right. Visitors build the program by **drag & drop with the mouse**.
- Blocks show valid Java syntax, with indentation handled automatically by nesting.
- **Distractor blocks:** a level may contain blocks that are not needed.
- **Editable parameters:** some numbers (e.g. loop count) can be changed by click or scroll wheel.
- **"Fix the bug" levels:** the program is prefilled and contains a single error.
- **Block budget:** a maximum number of blocks per level is optional.
- **Rating:** 1 star for solved, 2 for under the battery target, 3 for the minimum number of blocks.

## 5. Execution & Feedback

- **Run** executes the program with animation. The currently executed line is highlighted.
- **Step** executes one command at a time.
- **Speed slider** has three speeds.
- **Reset** returns the rover to its start position. The program is kept.
- Errors are shown in visitor-friendly language, e.g. "The rover drove into a crater!" or "Battery empty!". A small "Java" hint names the line.
- Success animation, star display and a "Next level" button.

## 6. Level Progression (default set)

1. **Landing:** sequence only (`vor`, `linksUm`, `probeNehmen`)
2. **Long Road:** repetition leads to the `for` loop
3. **Crater Field:** `if (vornKrater())`
4. **Unknown Distance:** `while (vornFrei())`
5. **Bonus – Mission Control:** define and reuse your own method
6. Additional levels: loaded from files, created by students

## 7. Input (mouse & keyboard, no touch)

- All interactions must be possible with the mouse alone. Drag targets must be large (min. 40 px block height).
- Keyboard shortcuts:
  - `F5` / `Enter`: Run
  - `F6`: Step
  - `R`: Reset
  - `Del`: remove the selected block
  - `Ctrl+Z`: Undo
- Right-click on a block deletes it.
- **Optional expert mode:** a text editor where advanced visitors type the code themselves. It is parsed against the same command set.

## 8. Kiosk Mode & Session

- Full-screen, with no window controls visible.
- Name entry at session start (max. 12 characters, simple word filter).
- **Auto-reset** after 60 s of inactivity leads back to the start screen.
- Hidden admin shortcut (e.g. `Ctrl+Shift+A` + PIN) to exit, reload levels, or reset the leaderboard.

## 9. Leaderboard (optional)

- Points = stars + time bonus, stored locally (JSON/SQLite).
- Optionally shown on a second screen or in a separate window.
- Optional: send solved levels to the Pixelwand server, so a solved level earns pixels.

## 10. Level Format

- One JSON file per level in a `levels/` folder, hot-reloadable via the admin shortcut.
- Content: title, intro text, grid size, tiles, rover start (position + direction), battery, allowed blocks (incl. distractors), optional prefilled program, star thresholds.

```json
{
  "id": "02-long-road",
  "title": "Long Road",
  "intro": "The base is far away. Do you really want to drag 'vor()' eight times?",
  "width": 10, "height": 3,
  "tiles": ["RRRRRRRRRR", "G G G G G G G G G B", "RRRRRRRRRR"],
  "rover": { "x": 0, "y": 1, "dir": "E" },
  "battery": 20,
  "palette": ["vor", "linksUm", "for", "probeNehmen"],
  "maxBlocks": 4,
  "stars": { "battery": 12, "blocks": 2 }
}
```

## 11. Technical Requirements

- Java 21+, JavaFX, built with Maven or Gradle.
- Clear separation:
  - `model`: world, rover, level
  - `program`: block AST and interpreter
  - `view`: JavaFX
- The interpreter runs on the AST, not via real Java compilation, which makes it safe and stoppable.
- **Endless-loop protection:** abort after N steps or when the battery is empty.
- Unit tests for the interpreter and level loader.
- Runs on one standard school PC with 1920×1080 resolution.

## 12. Nice to Have

- Two-player race mode (split screen, same level).
- Printable / QR "Mission certificate" showing the visitor's solution as Java code.
- Sound effects (rover motor, sample beep, crater crash), with a mute toggle.
- Level editor for students.
- Pixel-art graphics designed by students.

## 13. Out of Scope

- Network multiplayer, user accounts, persistent progress across days.
