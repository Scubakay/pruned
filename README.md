# Pruned

> Automatically create a pruned world download on Google Drive

Pruned automates the process for creating world downloads on Google Drive. Pruned does this 
by only uploading region data for chunks with a minimum inhabited time.

Note: Pruned is not a backup tool. It will not restore any backups
and backups made by Pruned can be incomplete.

## Setup

1. Add the mod to your instance
2. Follow the steps to add a client in https://developers.google.com/workspace/drive/api/quickstart/java
3. Save your `credentials.json` into `config/pruned`
4. Log in to your Google account using the `/pruned login` command

## Ignored files

The config file `config/prunedworlddownload.json` contains a list of files
that should be ignored. This can be used to prevent Pruned from uploading things
like large databases and other files that aren't necessary for a world download.

All files that are not ignored will be uploaded on sync.

Default ignore list:
- `prunedworlddownload_backupData.dat`: Don't upload pruned data.
- `.mca`: These files are handled by Pruned. Removing this will break Pruned.
- `.sqlite*`: Ledger and DistantHorizons use very large sqlite databases.
- `.dat_old`: Backup files used by Minecraft itself. These are not necessary for a world download.
- `.lock`: Not necessary for world downloads.

Items in this list should work like gitignores, but only features we needed have been implemented. 
If there are gitignore-style features that you need, please submit a ticket, so we can add it.

## Tips

- Need to quickly get certain chunks into the world download? Add the inhabitedTime or a range of chunks
using [Inhabitor](https://modrinth.com/mod/inhabitor)!