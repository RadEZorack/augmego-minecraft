# Deploy To A DigitalOcean Droplet With GitHub Actions

This repo is already close to droplet-ready. The production flow in this guide uses GitHub Actions to copy the repo to the server and then runs:

```sh
docker compose -f compose.droplet.yaml up -d
```

That means:

- GitHub Actions is your deploy trigger
- The droplet runs only the Minecraft server and Nginx
- The Bun backend can live somewhere else, such as DigitalOcean App Platform
- Minecraft world data, configs, and mods live outside the checked-out app directory so deploys do not wipe them
- The droplet uses a dedicated compose file instead of inheriting services from local development

## 1. Create the droplet

Use an Ubuntu droplet with enough RAM for the server size you want.

- Small test server: 2 vCPU / 4 GB RAM, with `MC_MEMORY=3G`
- More comfortable for modded Fabric: 4 vCPU / 8 GB RAM

Open these firewall ports on the droplet:

- `22/tcp` for SSH
- `80/tcp` for BlueMap through Nginx
- `25565/tcp` for Minecraft

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

## 4. Create the runtime env file on the droplet

Create `/opt/augmego-minecraft/env/minecraft.env`:

```dotenv
MC_VERSION=1.21.11
MC_MEMORY=3G
RCON_PASSWORD=replace-this
SERVER_NAME=Augmego Minecraft
MOTD=Welcome to Augmego Minecraft
WEB_BASE_URL=https://your-domain.example
WEB_ORIGINS=https://your-domain.example
```

On a 4 GB droplet, avoid setting `MC_MEMORY=4G`. The host still needs memory for Docker, Linux, and Nginx, and BlueMap can add pressure too. `3G` is a safer default until you move to a larger box.

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
3. It exports variables from `/opt/augmego-minecraft/env/minecraft.env`.
4. It runs the dedicated droplet compose file.
5. Docker starts or restarts only the `minecraft` and `nginx` services.

## 9. First deploy

Once the secrets are set:

1. Push your branch to `main`, or run the workflow manually from the Actions tab.
2. SSH into the droplet.
3. Check container status:

```sh
cd /opt/augmego-minecraft/app
docker compose -f compose.droplet.yaml ps
```

4. Check Minecraft logs:

```sh
cd /opt/augmego-minecraft/app
docker compose -f compose.droplet.yaml logs -f minecraft
```

5. Check BlueMap through Nginx in a browser:

```text
http://your-domain.example/
```

## 10. Recommended next improvements

After the basic deploy works, the next upgrades I would make are:

- Add HTTPS with Caddy or Nginx plus Let's Encrypt
- Point the backend-specific URLs at your DigitalOcean App Platform service
