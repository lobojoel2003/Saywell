# Boards and the Open Board Format

Saywell stores boards in its own JSON file and reads and writes the
[Open Board Format](https://www.openboardformat.org/docs) (OBF) to exchange them.

## Importing

**Settings › Boards › Import boards** accepts:

- `.obf`: one board as JSON. Pictures can be embedded (`data:` URIs) or online (`url`).
- `.obz`: a zip with `manifest.json`, several boards and their pictures.

Choose **Add** to keep your boards and add the new ones (a folder button to the imported home board
is put in the first free space on your home board), or **Replace all**. Both can be undone while
editing in the same session.

What carries over: labels, vocalizations, grid positions, background and border colours, pictures,
links between boards (`load_board` by id or path), hidden buttons, and these actions:

| OBF action | Saywell |
|---|---|
| `:clear` | Clear the message |
| `:backspace` | Delete the last word, ending or letter |
| `:space` | Start a new word (spelling) |
| `:speak` | Speak the message |
| `:home` | Go to the home board |
| `:back` | Go back one board (extension used by several apps) |
| `+text` | Add `text` to the last word (`+s`, `+ing`, or letters when spelling) |

Other actions are kept in the file but do nothing in Saywell. Sounds are not played.

Saywell is lenient with files from other apps: ids may be numbers, sizes may be fractions, colours
may be `rgb()`, `rgba()` (blended on white), `#rgb`, `#rrggbb` or names, and boards without a grid
are laid out in a square. Grids larger than 12 × 12 are fitted to 12 × 12; buttons that don't fit
are reported. Imported boards and buttons get new ids so they never clash with yours.

Pictures from symbol sets Saywell bundles (OpenMoji, `symbol.set = "openmoji"`) are linked to the
bundled symbol; others are copied into Saywell. Online pictures are downloaded once if there is a
connection (up to 5 MB each); the import reports any that couldn't be fetched.

Files are size-limited (64 MB per entry, 512 MB in total) and paths are never used to write files,
so a damaged or hostile file can't harm the device.

## Exporting

- **Back up all boards** writes an `.obz` with every board, links between them, and all pictures
  (OpenMoji symbols as SVG, photos as PNG or JPEG). This is the best backup format.
- **Share this board** writes the current board as a single `.obf` with pictures embedded. Links to
  other boards are kept by id but the other boards aren't included.

Colours are written as `rgb(r, g, b)`, and OpenMoji pictures carry their licence (CC BY-SA 4.0).

## Saywell's own storage

Inside the app, boards are saved as `files/data/boards.json` (written atomically; a damaged file
is kept as `boards.json.broken` and the starter boards are loaded instead). Photos are in
`files/images/`. The format is versioned (`version` field) for future migrations.
