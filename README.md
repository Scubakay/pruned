# Pruned

> Automatically create a pruned world download on WebDAV

Pruned automates the process for creating world downloads on WebDAV. Pruned does this
by only uploading region data for chunks with a minimum inhabited time.

Note: Pruned is not a backup tool. It will not restore any backups
and worlds uploaded by Pruned can be incomplete.

## Setup

Pruned can be used either client and server side. 

Create an account at a WebDAV provider like Koofr, or set up your own WebDAV server.

1. Open a world
   - Open a singleplayer world with Pruned added to the client
   - Join a server with Pruned added to the server
2. Go to the pause screen dialogs and click Pruned
3. Click load
4. Activate Pruned. 
   - Enter your WebDAV login credentials if this is the first time activating pruned.
   - Some WebDAV providers require you to use an application password

## Config

Config is stored in `config/pruned.json`.

- **uploadStrategy:** 
  - `INTERVAL`: Upload world on an interval, configurable with `uploadInterval`.
  - `SERVER_STOP`: Upload world when the server stops. This prevents the server from fully 
  shutting down until the world has uploaded.
  - `MANUAL` (default): Manually upload your world through the Pruned dialog or the 
  `/pruned upload` command.
- **uploadInterval:** The time in minutes between world uploads.
- **stopUploadOnServerStop:** When true, force uploads to stop. 
Ignored when using `SERVER_STOP` upload strategy.
- **autoAddInhabitedChunks:** When true, adds regions to the world download when their inhabited
time is larger than `inhabitedTime`.
- **inhabitedTime:** The time in minutes a chunk needs to be loaded before its region is automatically
added to the world download.
- **ignored:** See [Ignored files](#ignored-files)
- **permissionLevel:** The permission level a player needs to access anything Pruned related. Does 
nothing in singleplayer.
- **webDavEndpoint:** The endpoint of the WebDAV server
- **webDavUsername:** The username for the WebDAV server
- **webDavPassword:** The password for the WebDAV server. The password is encrypted, so you need to
configure it using the WebDAV Config dialog.


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