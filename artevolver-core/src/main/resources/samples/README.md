# Sample Atlas — Gamedev-Designed Tree

*Miyamoto: discovery. Wright: multiple paths. Kojima: secrets. Sid Meier: meaningful gates.*

## Real Images (Recommended)

**If Mona Lisa, Girl with a Pearl Earring, or other legendary art show as solid colors or stripes**, run the download script to replace gradient placeholders with real images:

```bash
# From project root
tools/download-samples.bat
```

Or manually: `cd artevolver-core` then `mvn exec:java -Dexec.mainClass="com.rndmodgames.evolver.clicker.DownloadSampleImages"`

- **Picsum** — Starter and Apprentice samples (landscape, ocean, forest, sunset, flowers, mountain, canyon, lake, meadow, beach) get real photos.
- **Wikimedia** — Legendary art (Mona Lisa, Starry Night, Great Wave, etc.). May hit rate limits (HTTP 429); add delays or retries if needed.
- Stored in `samples/{id}.jpg`. Missing files fall back to `GenerateSamplePlaceholders` (gradient placeholders).

## How Samples Are Added Today

- **Where images live**: `artevolver-core/src/main/resources/samples/{id}.jpg`
- **How they were added**: by running `DownloadSampleImages` (Wikimedia + Picsum) and committing the results.
- **Retro palettes today**: retro tabs (GB/GBC/Genesis/GBA/SNES) apply palette + resolution **at runtime** per `?preset=...&thumb=1`.

## Next Step (Automation Roadmap)

To fully scale the library and avoid manual downloads, the recommended workflow is:
- **One registry file** with IDs + source URLs + license
- **One build command** that downloads, resizes (720×468), and pre-renders:
  - base image
  - thumbnails
  - all retro-preset versions (palette + resolution)
- **One manifest** (`samples.json`) consumed by the server

If you want this automated pipeline now, I’ll wire it in.

## Iconic Art (Legendary Tier) — Manual Replacement

Replace placeholders with public-domain images from Wikimedia Commons if downloads fail:

| ID | Title | Artist | Source |
|----|-------|--------|--------|
| mona_lisa | Mona Lisa | Leonardo da Vinci, c.1503 | [Wikimedia](https://commons.wikimedia.org/wiki/File:Mona_Lisa,_by_Leonardo_da_Vinci,_from_C2RMF_retouched.jpg) |
| starry_night | The Starry Night | Van Gogh, 1889 | [Wikimedia](https://commons.wikimedia.org/wiki/File:Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg) |
| great_wave | The Great Wave off Kanagawa | Hokusai, c.1831 | [Wikimedia](https://commons.wikimedia.org/wiki/File:Great_Wave_off_Kanagawa2.jpg) |
| girl_pearl_earring | Girl with a Pearl Earring | Vermeer, c.1665 | [Wikimedia](https://commons.wikimedia.org/wiki/File:Jan_Vermeer_van_Delft_002.jpg) |
| birth_of_venus | The Birth of Venus | Botticelli, c.1485 | [Wikimedia](https://commons.wikimedia.org/wiki/File:Sandro_Botticelli_-_La_nascita_di_Venere_-_Google_Art_Project_-_edited.jpg) |
| american_gothic | American Gothic | Grant Wood, 1930 | [Wikimedia](https://commons.wikimedia.org/wiki/File:American_Gothic.jpg) |
| persistence_of_memory | The Persistence of Memory | Dalí, 1931 | Check copyright; many PD reproductions exist |

Resize to 720×468. Target **75–80%+ fitness** with Sherwin-Williams 4× palette.

## Legendary Expansion (Public Domain)

Additional public-domain legendary sets (downloaded via `tools/download-samples.bat`):

- **Legendary Photography:** `lunch_atop_skyscraper`, `migrant_mother`, `aldrin_moon`
- **Legendary Space:** `earthrise`, `blue_marble`
- **Legendary Cinema:** `nosferatu_orlok`, `metropolis_set`, `caligari_still`, `safety_last_ad`
- **Legendary Posters:** `uncle_sam_poster`, `weapons_for_liberty`, `wpa_national_parks`, `wpa_work_pays`

## Unlock Paths (OR logic)

Samples unlock when **any** condition is met:

- **Apprentice:** 500 EP **or** 1000 clicks → mountain, etc.
- **Veteran:** 1 ascension **or** 1 masterpiece → cityscape, etc.
- **Master:** Multiple paths (ascensions, masterpieces, EP, clicks)
- **Legendary:** 5 masterpieces **or** 10 ascensions → Mona Lisa; 50 GF **or** 10 masterpieces → Birth of Venus; etc.
- **Secret:** 20 miss streak **or** 100k clicks; 100k EP **or** 100k clicks; 15 ascensions **or** 15 masterpieces; 200 GF; 500 GF **or** 20 masterpieces

## Sample IDs (file-based)

Starter: landscape, ocean, forest, sunset, flowers  
Apprentice: mountain, canyon, lake, meadow, beach  
Veteran: cityscape, architecture, portrait, wildlife, night  
Master: abstract, macro, aerial, street, still_life  
Legendary: mona_lisa, starry_night, great_wave, girl_pearl_earring, birth_of_venus, american_gothic, persistence_of_memory  
Secret: secret_suffer, secret_grind, secret_ascended, secret_gilded, secret_omega

## Generated Samples

The **Generated** category (gradient_sunset, circles, checker, plasma, cityline, mosaic, etc.) uses programmatic fallbacks. Always available for testing.

## Real World (Photo-Inspired)

The **Real World** category (countryside, seascape2, urban_night, autumn_forest, winter_snow, spring_garden, desert_dunes, tropical, river_valley, coastal_cliff) uses programmatic scene generators — rich gradients and compositions that look great when quantized. Always available.

## Custom Images (My Images)

Custom uploads are saved to `~/.artevolver/clicker-uploads/{fingerprint}.png` on first use. They appear in the init overlay under "My Images" and in the Gallery with a "Play again" button for easy re-use.
