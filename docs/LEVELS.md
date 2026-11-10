# Levels bauen – Anleitung für Schüler:innen

Jedes Level ist eine JSON-Datei im Ordner `levels/` neben `mars-rover.jar`.
Das Spiel spielt **alle** Levels im Ordner, sortiert nach Dateinamen (`01-…`, `02-…`, …).

> ⚠️ Jedes zusätzliche Level verlängert jede Besucher-Runde. Eine Runde soll 3–5 Minuten dauern –
> also lieber ein Level **austauschen** (alte Datei in einen anderen Ordner verschieben) als
> immer mehr dazuzugeben.

## Neues Level ausprobieren

1. Datei in `levels/` speichern, z. B. `05-spirale.json`.
2. Im Spiel **Strg + Umschalt + A** drücken, PIN eingeben.
3. **Levels neu laden** klicken.
4. Fehler stehen direkt darunter (Datei + Grund). Kaputte Dateien werden übersprungen, das Spiel läuft weiter.

## Beispiel

```json
{
  "id": "05-spirale",
  "title": "Spirale",
  "intro": "Ein kurzer Satz, der neugierig macht.",
  "width": 6,
  "height": 4,
  "tiles": [
    "RRRRRR",
    "GGSGGB",
    "GCGGRG",
    "GGGGGG"
  ],
  "rover": { "x": 0, "y": 1, "dir": "E" },
  "battery": 20,
  "palette": ["vor", "linksUm", "rechtsUm", "probeNehmen", "for:3", "if:vornKrater"],
  "maxBlocks": 8,
  "stars": { "battery": 10, "blocks": 5 }
}
```

## Felder

| Feld | Pflicht | Bedeutung |
|---|---|---|
| `id` | ja | Eindeutiger Name, am besten gleich wie der Dateiname |
| `title` | ja | Überschrift im Spiel |
| `intro` | nein | Ein, zwei Sätze Erklärung |
| `width`, `height` | ja | Größe, höchstens 20 × 12 |
| `tiles` | ja | Eine Zeichenkette pro Zeile, **genau `width` Zeichen, keine Leerzeichen** |
| `rover` | ja | Start: `x` (Spalte, ab 0), `y` (Zeile, ab 0, oben), `dir` = `N` `E` `S` `W` |
| `battery` | ja | Startbatterie |
| `palette` | ja | Welche Blöcke es gibt (siehe unten) |
| `maxBlocks` | nein | Höchstzahl Blöcke im Programm |
| `stars.battery` | ja | ★★ wenn höchstens so viel Batterie verbraucht |
| `stars.blocks` | ja | ★★★ wenn zusätzlich höchstens so viele Blöcke |

Tippfehler in Feldnamen (z. B. `maxblocks`) werden als Fehler gemeldet.

## Felder der Karte (`tiles`)

| Zeichen | Feld | Wirkung |
|---|---|---|
| `G` | Boden | befahrbar |
| `R` | Fels | blockiert – Rover fährt dagegen = Fehler |
| `C` | Krater | Rover fällt hinein = Fehler |
| `S` | Probe | befahrbar, mit `probeNehmen()` einsammeln |
| `B` | Basis | Ziel – **genau eine** pro Level |
| `D` | Staub | befahrbar, kostet aber 2 Batterie |

Gewonnen ist, wenn am **Ende des Programms** alle Proben eingesammelt sind und der Rover auf der Basis steht.
Der Kartenrand zählt wie ein Fels.

## Blöcke (`palette`)

| Eintrag | Block im Spiel |
|---|---|
| `"vor"`, `"linksUm"`, `"rechtsUm"`, `"probeNehmen"` | die Befehle |
| `"for:3"` | `for (int i = 0; i < 3; i++) { }` – die Zahl (1–20) können Besucher ändern |
| `"while:vornFrei"` | `while (vornFrei()) { }` |
| `"while:!amZiel"` | `while (!amZiel()) { }` – `!` heißt „nicht“ |
| `"if:vornKrater"` | `if (vornKrater()) { }` |
| `"ifelse:vornKrater"` | `if (vornKrater()) { } else { }` |

Sensoren: `vornFrei` (kein Fels, kein Krater, nicht der Rand), `vornKrater`, `probeDa`, `amZiel`.

**Ablenkungs-Blöcke:** Einfach Blöcke in die Palette schreiben, die man nicht braucht.

## Tipps für gute Levels

- **Erst selbst lösen**, dann `stars.battery` und `stars.blocks` auf deine beste Lösung setzen.
- Mit `maxBlocks` erzwingst du Schleifen: Wenn 9 × `vor()` nicht in 4 Blöcke passt, braucht man `for`.
- Lass das Ziel klar erkennbar: Probe und Basis gut sichtbar, nicht hinter Felsen versteckt.
- Ein Level soll in 1–2 Minuten lösbar sein.
