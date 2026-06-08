# Modrinth Gallery Assets

This folder is the source of truth for Modrinth project gallery images.

## Layout

- `gallery/InvSort`
- `gallery/InvSearch`
- `gallery/InvCatalogue`

Each mod folder contains the images that should be uploaded to that mod's
Modrinth gallery. The leading number controls display order on Modrinth.

The `banner` and `description_images` folders are selectors, not separate
upload sets:

- `banner` contains the image that should be marked as the featured gallery
  image.
- `description_images` contains copies of gallery images that should be embedded
  in the long Modrinth description.

Do not upload the selector copies separately. The sync script uploads only the
root images in each mod folder, then resolves description image placeholders to
the uploaded Modrinth CDN URLs.

## Metadata

Image titles and descriptions live in `gallery/metadata.json`. Keep the image
file names aligned with that manifest.

## Sync

Use the project-page sync script after changing gallery images, image metadata,
or `gradle/modrinth-project-pages.md`:

```powershell
.\scripts\sync-modrinth-project-pages.ps1 -DryRun
.\scripts\sync-modrinth-project-pages.ps1 -ReplaceGallery
```

The script reads `MODRINTH_TOKEN` from the process environment or `.env`. It
updates page-level Modrinth metadata only: project summaries, long descriptions,
and gallery images. It does not upload mod versions or release jars.
