# Test Plan – Mars Rover (open day 2026-11-13)

Run this on the **kiosk PC** on Thursday, then again briefly on Friday morning.
Tick every box; anything that fails goes to the "Found" list at the end.

## 0. Install

- [ ] JDK 25 installed: `java -version` in a terminal shows 25.
- [ ] `target/mars-rover-dist.zip` copied and unzipped, e.g. to `C:\MarsRover\`.
      The folder contains `mars-rover.jar`, `start.bat`, `levels\`, `data\`, `LEVELS.md`.
- [ ] `data\config.properties`: **`adminPin` changed** from `1234`.
- [ ] Double-click `start.bat` → start screen in full screen, no console window.

## 1. Windows settings (kiosk PC)

- [ ] Display resolution 1920×1080 and **scaling 100 %**. At 125 % the layout gets cramped. Check it at least once.
- [ ] Sleep, screen saver and screen-off disabled (Power settings).
- [ ] Windows updates paused for the day; notifications off ("Do not disturb").
- [ ] Optional: `start.bat` in the Autostart folder (`shell:startup`), so a reboot brings the game back.
- [ ] Optional, strongest lock: a separate Windows account with "Assigned Access" (kiosk mode).
      The app itself cannot block the Windows key or Ctrl+Alt+Del.

## 2. Kiosk lock

- [ ] Esc does not leave full screen.
- [ ] Alt+F4 does nothing.
- [ ] Alt+Tab away and back → game is full screen again.
- [ ] Ctrl+Shift+A → PIN prompt. Wrong PIN 3× → "30 s gesperrt".
- [ ] Correct PIN → "Programm beenden" closes the app.

## 3. Visitor session (SPEC §11 criteria 2 and 3)

- [ ] Name: empty, 13+ characters (typing stops), "Max!" and a bad word are refused with a message.
- [ ] Level 1 solvable by drag & drop only. **Newcomer test: under 1 minute without help.**
- [ ] Clicking a palette block appends it; dragging back to the palette deletes; right-click deletes.
- [ ] `for` number: left click +1, right click −1, scroll wheel; stays within 1–20.
- [ ] Block limit: level 2 refuses a 5th block, counter flashes red.
- [ ] Run / Step / Reset, all three speeds; F5, Enter, F6, R, Del work.
- [ ] Failures show a red banner + "Zeile N": crater, rock, map edge, battery empty, no sample,
      endless loop (`while (vornFrei()) { linksUm(); }` with nothing else), samples left, not at base.
- [ ] Success card with 1, 2 and 3 stars; "Nochmal" improves stars; last level says "Mission beenden".
- [ ] End screen: stars per level, total, time, rank; highlighted row on the leaderboard.
- [ ] **Full session with a newcomer: 3–5 minutes.**

## 4. Inactivity and robustness (criterion 4)

- [ ] Leave a level idle for 60 s → start screen. If a level was solved, the result is on the leaderboard.
- [ ] A running program does **not** trigger the reset (slow speed, long program).
- [ ] Admin panel left open → closes itself after 60 s idle.
- [ ] 30 minutes of random clicking, dragging, typing and key mashing: no crash, no freeze.
- [ ] Afterwards `data\errors.log` is absent or empty. If not, keep the file for analysis.

## 5. Levels and leaderboard (criteria 5 and 6)

- [ ] Copy a new level into `levels\`, admin → "Levels neu laden" → it appears in the next session.
- [ ] Copy a broken JSON into `levels\` → reload shows file + reason; the game keeps running.
- [ ] Restart the app → leaderboard is still there.
- [ ] Admin → "Bestenliste löschen" (two clicks) → empty board.
- [ ] Before opening: leaderboard cleared, test levels removed from `levels\`.

## Found

| # | What | Severity | Fixed? |
|---|---|---|---|
| | | | |
