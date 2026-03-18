# Mods Manifest

This folder is for server-side mod jars that should exist locally but should not be committed to git.

## Installed mods

| Mod | Version | Jar file |
| --- | --- | --- |
| BlueMap (Fabric) | 5.16 | `bluemap-5.16-fabric.jar` |

## Notes

- Keep `minecraft/mods/*.jar` out of git
- Update this file when you add, remove, or upgrade a mod
- If a mod has extra required dependencies, list them here too

scp -i ~/.ssh/augmego_do_github_actions -r /Users/YOUR_USERNAME/Documents/augmego-minecraft/minecraft/mods/* root@YOUR_DROPLET_IP:/opt/augmego-minecraft/minecraft/mods/
