# Demotivator

[![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-brightgreen?style=flat-square&logo=minecraft)](README.md)
[![Platform](https://img.shields.io/badge/Platform-Fabric-blue?style=flat-square&logo=fabric)](README.md)
[![Java Target](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](README.md)
[![Mod Version](https://img.shields.io/badge/Version-1.1.0-purple?style=flat-square)](README.md)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

Client-side Fabric mod that turns a screenshot into a classic demotivator poster: black background, thin white frame, large serif title and a smaller subtitle, all from one chat command.

**Руководство / User guide:** [Русский](docs/USER_GUIDE_RU.md) · [English](docs/USER_GUIDE_EN.md)

## Русский

### Что это

`Demotivator` (mod id `demotivator`) — клиентский Fabric-мод для Minecraft 26.3. Команда `/demotivate <текст>` закрывает чат, на время снимка скрывает HUD, делает скриншот и собирает из него демотиватор в PNG. Сборка картинки и запись файла идут в фоновом потоке, игра не подвисает.

### Команды

| Команда | Что делает |
|---|---|
| `/demotivate <заголовок>` | Скриншот в рамке демотиватора |
| `/demotivate <заголовок> \| <подпись>` | Всё после `\|` становится мелкой подписью |
| `\n` внутри текста | Ручной перенос строки |
| `/demotivator` или `/demotivator config` | Настройки |
| `/demotivator last` | Показать последний демотиватор |
| `/demotivator folder` | Открыть папку с картинками |
| `/demotivator help` | Подсказка по командам |

Пример: `/demotivate Копал прямо вниз | и нашёл лаву. Опыт бесценен.`

Картинки сохраняются в `screenshots/demotivators/demotivator_<дата>.png`. В чат приходит кликабельная ссылка на файл и на папку; после создания открывается экран превью с кнопками «Открыть картинку», «Открыть папку», «Настройки».

### Управление

| Действие | Клавиша по умолчанию | Переназначение |
|---|---|---|
| Открыть настройки | не назначена | Настройки → Управление → Demotivator |
| Показать последний демотиватор | не назначена | Настройки → Управление → Demotivator |

Настройки также открываются через **Mod Menu**.

### Настройки

Вкладка **Быстрый доступ**: скрывать HUD на снимке, показывать превью, сообщение в чат, задержка снимка в тиках, папка внутри `screenshots/`, подпись по умолчанию (используется, если в команде нет `|`).

Вкладка **Стиль постера**: ширина картинки, внешний отступ, толщина и зазор рамки, размеры заголовка и подписи (в % от ширины картинки), заголовок капсом, шрифты заголовка и подписи (любой установленный в системе шрифт; если шрифта нет, используется стандартный с засечками / без засечек), цвета фона, рамки, заголовка и подписи в формате `#RRGGBB` с образцом цвета.

Некорректные значения подсвечиваются красным, а «Сохранить» блокируется до исправления. Файл `config/demotivator.json` пишется атомарно с резервной копией `.bak`.

### Установка

1. **Fabric Loader** `0.19.3+` и **Fabric API**.
2. Скопируйте `demotivator-1.1.0.jar` из `build/libs/` в папку `mods/`.
3. **Mod Menu** — опционально.

**Требования:** Minecraft `26.3` · Java `25` · только клиент.

### Сборка

```powershell
.\gradlew.bat clean build
```

Готовый JAR: `build/libs/demotivator-1.1.0.jar`.

---

## English

### What It Is

`Demotivator` (mod id `demotivator`) is a client-side Fabric mod for Minecraft 26.3. `/demotivate <text>` closes chat, hides the HUD for the shot, takes a screenshot and composes it into a demotivator PNG. Composition and file writing run on a background thread, so the game does not stall.

### Commands

| Command | What it does |
|---|---|
| `/demotivate <title>` | Screenshot framed as a demotivator |
| `/demotivate <title> \| <subtitle>` | Everything after `\|` becomes the smaller subtitle |
| `\n` inside the text | Manual line break |
| `/demotivator` or `/demotivator config` | Settings |
| `/demotivator last` | Show the last demotivator |
| `/demotivator folder` | Open the output folder |
| `/demotivator help` | Command help |

Images are saved to `screenshots/demotivators/demotivator_<date>.png`. Chat gets clickable links to the file and its folder, and a preview screen opens with Open image, Open folder and Settings buttons.

### Controls

| Action | Default key | Rebind |
|---|---|---|
| Open settings | unbound | Options → Controls → Demotivator |
| Show last demotivator | unbound | Options → Controls → Demotivator |

Settings are also available through **Mod Menu**.

### Configuration

**Quick Access** tab: hide HUD in the shot, show preview, chat message, capture delay in ticks, folder inside `screenshots/`, default subtitle (used when the command has no `|`).

**Poster Style** tab: image width, outer margin, frame thickness and gap, title and subtitle size (percent of image width), uppercase title, title and subtitle fonts (any installed system font; a missing font falls back to the default serif / sans-serif), and background, frame, title and subtitle colors as `#RRGGBB` with a color swatch.

Invalid values turn red and Save stays disabled until they are fixed. `config/demotivator.json` is written atomically with a `.bak` backup.

### Installation

1. **Fabric Loader** `0.19.3+` and **Fabric API**.
2. Copy `demotivator-1.1.0.jar` from `build/libs/` into `mods/`.
3. **Mod Menu** — optional.

**Requirements:** Minecraft `26.3` · Java `25` · client-only.

### Building

```powershell
.\gradlew.bat clean build
```

Output JAR: `build/libs/demotivator-1.1.0.jar`.

## Лицензия / License

[MIT](LICENSE). Developed by LTS_Server.
