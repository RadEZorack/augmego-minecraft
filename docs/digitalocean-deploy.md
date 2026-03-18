# Deploy To A DigitalOcean Droplet With GitHub Actions

This repo is already close to droplet-ready. The production flow in this guide uses GitHub Actions to copy the repo to the server and then runs:

```sh
docker compose -f compose.yaml -f compose.prod.yaml up -d --build
```

That means:

- GitHub Actions is your deploy trigger
- The droplet is the machine that builds and runs the containers
- Minecraft world data, configs, and mods live outside the checked-out app directory so deploys do not wipe them

## 1. Create the droplet

Use an Ubuntu droplet with enough RAM for the server size you want.

- Small test server: 2 vCPU / 4 GB RAM
- More comfortable for modded Fabric: 4 vCPU / 8 GB RAM

Open these firewall ports on the droplet:

- `22/tcp` for SSH
- `80/tcp` for BlueMap through Nginx
- `25565/tcp` for Minecraft
- `3000/tcp` only if you want direct access to the backend API

## 2. Install Docker on the droplet

SSH into the droplet and install Docker plus Compose:

```sh
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
```

Log out and SSH back in so the Docker group change applies.

## 3. Prepare the server directories

Run this once on the droplet:

```sh
sudo mkdir -p /opt/augmego-minecraft/app
sudo mkdir -p /opt/augmego-minecraft/env
sudo mkdir -p /opt/augmego-minecraft/minecraft/data
sudo mkdir -p /opt/augmego-minecraft/minecraft/mods
sudo mkdir -p /opt/augmego-minecraft/minecraft/config
sudo chown -R $USER:$USER /opt/augmego-minecraft
```

## 4. Create the runtime env files on the droplet

Create `/opt/augmego-minecraft/env/minecraft.env`:

```dotenv
MC_VERSION=1.21.11
MC_MEMORY=4G
RCON_PASSWORD=replace-this
SERVER_NAME=Augmego Minecraft
MOTD=Welcome to Augmego Minecraft
WEB_BASE_URL=https://your-domain.example
WEB_ORIGINS=https://your-domain.example
```

Create `/opt/augmego-minecraft/env/backend.env` with the backend secrets your `core` service needs. At minimum:

```dotenv
PORT=3000
DATABASE_URL=postgresql://postgres:postgres@postgres:5432/minecraft?schema=public
WEB_BASE_URL=https://your-domain.example
WEB_ORIGINS=https://your-domain.example
```

If your backend uses additional secrets in [`core/src/index.ts`](/Users/travismiller/Documents/augmego-minecraft/core/src/index.ts), put them in this file too.

## 5. Put the Fabric mod jars on the droplet

Copy your server-side mod jars into:

```text
/opt/augmego-minecraft/minecraft/mods
```

For this stack that usually includes:

- Fabric API
- BlueMap for Fabric
- Any other server-side Fabric mods you want enabled

## 6. Update Nginx for your real hostname

Edit [`nginx/default.conf`](/Users/travismiller/Documents/augmego-minecraft/nginx/default.conf) and replace:

```nginx
server_name _;
```

with your real domain, for example:

```nginx
server_name map.example.com;
```

Commit that change before you deploy so the workflow sends the updated config to the droplet.

## 7. Add GitHub repository secrets

In your GitHub repo, add these Actions secrets:

- `DO_HOST`: your droplet IP or hostname
- `DO_USER`: the SSH user on the droplet
- `DO_SSH_PRIVATE_KEY`: the private key GitHub Actions will use to SSH into the droplet
- `DO_PORT`: optional, only if you do not use port `22`

The workflow file is [`deploy-digitalocean.yml`](/Users/travismiller/Documents/augmego-minecraft/.github/workflows/deploy-digitalocean.yml).

## 8. How the deploy works

On every push to `main`, or when manually triggered:

1. GitHub Actions checks out the repo.
2. It copies the repo contents to `/opt/augmego-minecraft/app` on the droplet.
3. It exports variables from the two droplet env files.
4. It runs Docker Compose with the base file plus the production override.
5. Docker rebuilds the backend image and restarts the stack.

## 9. First deploy

Once the secrets are set:

1. Push your branch to `main`, or run the workflow manually from the Actions tab.
2. SSH into the droplet.
3. Check container status:

```sh
cd /opt/augmego-minecraft/app
docker compose -f compose.yaml -f compose.prod.yaml ps
```

4. Check Minecraft logs:

```sh
cd /opt/augmego-minecraft/app
docker compose -f compose.yaml -f compose.prod.yaml logs -f minecraft
```

5. Check BlueMap through Nginx in a browser:

```text
http://your-domain.example/
```

## 10. Recommended next improvements

After the basic deploy works, the next upgrades I would make are:

- Add HTTPS with Caddy or Nginx plus Let's Encrypt
- Move PostgreSQL credentials off the default `postgres/postgres`
- Build and push the backend image from GitHub Actions instead of building on the droplet
- Restrict `3000/tcp` in the firewall if the backend does not need public access
