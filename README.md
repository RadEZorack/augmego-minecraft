# Minecraft Stack

Minimal local-first stack for:

- Docker Compose
- Fabric Minecraft server
- BlueMap mod support
- Nginx reverse proxy

## Structure

```text
.
├─ compose.yaml
├─ .env
├─ nginx/
│  └─ default.conf
└─ minecraft/
   ├─ mods/
   ├─ config/
   └─ data/
```

## What goes where

- Put the BlueMap Fabric jar in `minecraft/mods/`
- Put Fabric API and any other Fabric mod jars in `minecraft/mods/`
- Minecraft world and server files persist in `minecraft/data/`

## First boot

1. Add your mod jars to `minecraft/mods/`
2. Start the stack:

   ```sh
   docker compose up -d
   ```

3. Watch the server logs:

   ```sh
   docker compose logs -f minecraft
   ```

4. Let BlueMap generate its config on first boot
5. Inspect the generated BlueMap config and confirm its web server port is `8100`
6. Restart the stack if you changed the BlueMap config

## Local addresses

- Minecraft: `localhost:25565`
- BlueMap via Nginx: <http://localhost:8080>

## Rebuild The Client Mod Jar

From the repo root, run:

```sh
JAVA_HOME=$PWD/.jdk/jdk-21.0.10+7/Contents/Home GRADLE_USER_HOME=$PWD/.gradle-home ./gradlew :minecraft-mod:build
```

The rebuilt client mod jar will be written to:

```sh
minecraft-mod/build/libs/augmego-avatar-poc-0.0.1.jar
```

## Stop and restart

Stop everything:

```sh
docker compose down
```

Start again:

```sh
docker compose up -d
```

## Later for DigitalOcean

This same setup should move over with minimal changes. You will typically:

- Open `25565/tcp` for Minecraft
- Open `80/tcp` for the BlueMap web view
- Point a domain or subdomain at the droplet
- Replace `server_name _;` in `nginx/default.conf` with your real hostname
- Add HTTPS once the basic setup is working

## Notes

BlueMap config paths and defaults can vary by version, so this stack intentionally lets BlueMap generate its own config first instead of hardcoding a guessed file.
