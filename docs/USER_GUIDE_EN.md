# Demotivator — User Guide

Mod version: 1.1.0 · Minecraft 26.3 · Fabric · client-only

[Русская версия](USER_GUIDE_RU.md)

## Contents

1. [What the mod does](#what-the-mod-does)
2. [Installation](#installation)
3. [Your first demotivator](#your-first-demotivator)
4. [Commands](#commands)
5. [Writing the text](#writing-the-text)
6. [The preview screen](#the-preview-screen)
7. [Settings](#settings)
8. [Files](#files)
9. [FAQ](#faq)
10. [Building from source](#building-from-source)

## What the mod does

One chat command turns what you see in game into a classic demotivator poster: the screenshot on a black background inside a thin white frame, with a large serif title and a smaller subtitle below it.

- Before the shot the mod closes chat and briefly hides the HUD, so only the world is in the picture.
- The image is composed and written on a background thread, so the game does not stall even at large sizes.
- The PNG goes to your screenshots folder and a preview opens right away.
- Fonts, sizes, margins and colors are configurable.

The mod runs only on your computer. The server does not need it and learns nothing about the shot: `/demotivate` is handled by the client and never sent to the chat.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) `0.19.3` or newer for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3 into the `mods` folder.
3. Put `demotivator-1.1.0.jar` into `mods` (see [Building from source](#building-from-source)).
4. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) to open the settings from the mod list.

Java 25 is required.

## Your first demotivator

1. Frame the view you want.
2. Open chat and type:

   ```
   /demotivate Dug straight down | and found lava. Priceless experience.
   ```

3. Chat closes and the shot is taken 3 ticks later without the HUD.
4. The preview opens, and chat gets a message with links to the file and the folder.

## Commands

| Command | What it does |
|---|---|
| `/demotivate <title>` | a shot with a title only |
| `/demotivate <title> \| <subtitle>` | a title and a smaller subtitle below it |
| `/demotivator` | open the settings |
| `/demotivator config` | the same |
| `/demotivator last` | show the last demotivator |
| `/demotivator folder` | open the output folder |
| `/demotivator help` | short help in chat |

The Open Demotivator settings and Show last demotivator keys are in **Options → Controls → Demotivator**. They are unbound by default so they do not clash with other mods.

## Writing the text

- Everything before the first `|` is the title, everything after it is the subtitle.
- Without `|` the subtitle comes from the Default subtitle setting (empty by default).
- `\n` inside the text breaks the line, for example `/demotivate First line\nSecond line`.
- Long text wraps by words on its own.
- An empty command (`/demotivate` without text) takes no shot and explains how to add text.

## The preview screen

After the shot a screen shows the image, its name and its size in pixels. Buttons:

- **Open image** opens the PNG in your default program;
- **Open folder** opens the demotivator folder;
- **Settings** opens the mod settings;
- **Close** or `Esc`.

With the preview turned off the image is still saved and the link still arrives in chat. `/demotivator last` opens the last one again.

## Settings

Open the settings with `/demotivator`, the key or Mod Menu. An invalid value turns red, and Save stays disabled until you fix it. Reset to defaults restores the defaults, Cancel closes without saving.

### Quick Access tab

| Setting | Default | What it does |
|---|---|---|
| Hide HUD in shot | on | hide the interface while the shot is taken |
| Show preview | on | open the preview screen after the shot |
| Chat message | on | post links to the file and folder in chat |
| Capture delay (ticks) | 3 | ticks to wait after chat closes, 1 to 60. Raise it if chat or the HUD ends up in the shot |
| Folder inside screenshots/ | `demotivators` | where images go. Letters, digits, spaces, dots, dashes and underscores are allowed |
| Default subtitle | empty | the subtitle when the command has no `\|`, up to 128 characters |

### Poster Style tab

| Setting | Default | Range | What it does |
|---|---|---|---|
| Image width (px) | 1000 | 320–3840 | width of the final PNG; the height follows the shot and the text |
| Outer margin (%) | 10 | 2–30 | space around the shot, in percent of the width |
| Frame thickness (px) | 3 | 0–40 | width of the white frame; 0 means no frame |
| Frame gap (px) | 4 | 0–40 | space between the shot and the frame |
| Title size (%) | 8 | 2–20 | title letter height in percent of the width |
| Subtitle size (%) | 3.6 | 1–12 | subtitle letter height |
| Uppercase title | off | | write the title in capitals |
| Title font | Times New Roman | any system font | falls back to the default serif font if missing |
| Subtitle font | Arial | any system font | falls back to the default sans-serif font if missing |
| Background color | `#000000` | `#RRGGBB` | a swatch next to the field shows the color |
| Frame color | `#FFFFFF` | `#RRGGBB` | |
| Title color | `#FFFFFF` | `#RRGGBB` | |
| Subtitle color | `#FFFFFF` | `#RRGGBB` | |

If no installed font has that name, a warning appears under the field, but you can still save.

## Files

- `screenshots/demotivators/demotivator_YYYY-MM-DD_HH.mm.ss.png` holds the images. Several in the same second get a number appended.
- `config/demotivator.json` holds the settings. It is written atomically with a backup `demotivator.json.bak`, which the mod reads if the main file is damaged.

## FAQ

**Chat or the HUD is in the shot.** Raise Capture delay to 5–10 ticks. This happens with mods that draw their own interface a little later.

**"The shot was cancelled because a screen stayed open."** The mod waits up to 10 seconds for an open screen (an inventory, for example) to close. Close it and run the command again.

**"The demotivator took too long and was cancelled."** Composition is given 30 seconds. Usually it means a very large width on a slow computer. Lower Image width.

**"A demotivator is already being made."** The previous command has not finished yet. Wait a moment.

**Cyrillic letters do not show.** The chosen font has no Cyrillic. Pick one that does, such as Times New Roman, Arial, Georgia or Segoe UI.

**Can other players see the command?** No. `/demotivate` is handled by your client and is not sent to the server.

## Building from source

JDK 25 is required.

```powershell
git clone https://github.com/ServChan/Demotivator.git
cd Demotivator
.\gradlew.bat build
```

On Linux and macOS use `./gradlew`. The jar is `build/libs/demotivator-1.1.0.jar`.
