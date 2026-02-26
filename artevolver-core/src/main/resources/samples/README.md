# Sample Atlas — Real Photos

The default Sample Atlas tree uses **real-life, open-source, and free images** for great fitness scores on the platform.

## Target Fitness: 75–80%+

Good samples achieve **75% to 80%+ fitness** with the default setup:
- **Palette:** Sherwin-Williams (4×)
- **Grid:** 80×53 triangles
- **Resolution:** 720×468 px

We can measure this because we have access to the palette, triangle count, and target size. Fitness is computed by `DeltaFitnessEngine` when you load an image.

## How to Curate

1. **Add or replace images** — Use filenames matching the sample ids (e.g. `landscape_01.jpg`, `ocean_01.jpg`).
2. **Test in the app** — Start the clicker with that sample, check the initial fitness (after Smart or LAP init).
3. **Keep images that reach 75–80%+** — These will feel rewarding to evolve.

## Free / Open-Source Sources

- [Unsplash](https://unsplash.com) — Free to use, no attribution required
- [Pexels](https://www.pexels.com) — Free stock photos and videos
- [Pixabay](https://pixabay.com) — Free images and videos

Look for **CC0** (Public Domain) or **free for commercial use** licenses. Resize to 720×468 or similar aspect ratio for best results.

## Sample IDs (file-based)

| ID | Category |
|----|----------|
| landscape_01, ocean_01, forest_01, sunset_01, flowers_01 | Starter |
| mountain_01, canyon_01, lake_01, meadow_01, beach_01 | Apprentice |
| cityscape_01, portrait_01, architecture_01, wildlife_01, night_01 | Veteran |
| abstract_01, macro_01, aerial_01, street_01, still_life_01 | Master |
| secret_01 … secret_05 | Secret |

Supported formats: `.jpg`, `.jpeg`, `.png`

## Generated Samples

The **Generated** category (gradient_sunset, circles, checker, etc.) uses programmatic fallbacks and does **not** load from files. Those remain always available for testing — samples are always re-pickable even after completion.
