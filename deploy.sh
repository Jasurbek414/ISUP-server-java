#!/bin/bash
# Universal ISUP Server - VPS Deploy Script
# Usage: ./deploy.sh root@SERVER_IP [--env /path/to/.env]

set -e

SERVER=$1
ENV_FILE=""

if [ -z "$SERVER" ]; then
    echo "Usage: $0 user@SERVER_IP [--env /path/to/.env]"
    exit 1
fi

# Parse optional --env argument
shift
while [[ $# -gt 0 ]]; do
    case $1 in
        --env)
            ENV_FILE="$2"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1"
            exit 1
            ;;
    esac
done

REMOTE_DIR="/opt/isup-server"
echo "==> Deploying ISUP Server to $SERVER:$REMOTE_DIR"

# Step 1: Check SSH connectivity
echo "==> Checking SSH connectivity..."
if ! ssh -o ConnectTimeout=10 -o BatchMode=yes "$SERVER" "echo OK" 2>/dev/null; then
    echo "ERROR: Cannot connect to $SERVER via SSH"
    exit 1
fi
echo "    SSH OK"

# Step 2: Install Docker if not present (Ubuntu 22.04)
echo "==> Checking Docker on remote server..."
ssh "$SERVER" bash <<'REMOTE_SETUP'
if ! command -v docker &>/dev/null; then
    echo "Installing Docker..."
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg lsb-release
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
        https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
        > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    systemctl enable docker
    systemctl start docker
    echo "Docker installed successfully"
else
    echo "Docker already installed: $(docker --version)"
fi
REMOTE_SETUP

# Step 3: Create remote directory
echo "==> Creating $REMOTE_DIR on remote..."
ssh "$SERVER" "mkdir -p $REMOTE_DIR/backups"

# Step 4: Copy project files via rsync (excluding build artifacts)
echo "==> Syncing project files..."
rsync -az --progress \
    --exclude 'build/' \
    --exclude '.gradle/' \
    --exclude '.git/' \
    --exclude '*.class' \
    --exclude 'backups/*.sql' \
    . "$SERVER:$REMOTE_DIR/"

# Step 5: Copy or create .env on remote
echo "==> Setting up .env on remote..."
if [ -n "$ENV_FILE" ] && [ -f "$ENV_FILE" ]; then
    echo "    Copying provided .env file..."
    scp "$ENV_FILE" "$SERVER:$REMOTE_DIR/.env"
elif ssh "$SERVER" "[ -f $REMOTE_DIR/.env ]"; then
    echo "    .env already exists on remote, keeping it"
else
    echo "    Creating .env from .env.example..."
    ssh "$SERVER" "cp $REMOTE_DIR/.env.example $REMOTE_DIR/.env"
    echo "    WARNING: .env created from defaults. Update ADMIN_SECRET and DB passwords!"
fi

# Step 6: Run docker compose up
echo "==> Building and starting containers..."
ssh "$SERVER" "cd $REMOTE_DIR && docker compose build --no-cache && docker compose down --remove-orphans && docker compose up -d"

# Step 7: Wait for health check to pass
echo "==> Waiting for health check..."
MAX_ATTEMPTS=30
ATTEMPT=0
until ssh "$SERVER" "curl -sf http://localhost:8090/health | grep -q '\"status\":\"UP\"'" 2>/dev/null; do
    ATTEMPT=$((ATTEMPT+1))
    if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
        echo "ERROR: Health check did not pass after ${MAX_ATTEMPTS} attempts"
        ssh "$SERVER" "cd $REMOTE_DIR && docker compose logs --tail=50 isup-server"
        exit 1
    fi
    echo "    Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting 10s..."
    sleep 10
done

echo ""
echo "==> Deployment successful!"
echo ""
# Step 8: Show final status
ssh "$SERVER" "cd $REMOTE_DIR && docker compose ps"
echo ""
echo "Endpoints:"
echo "  API / Web UI: http://$(echo $SERVER | cut -d@ -f2):8090"
echo "  Health:       http://$(echo $SERVER | cut -d@ -f2):8090/health"
echo "  Metrics:      http://$(echo $SERVER | cut -d@ -f2):8090/metrics"
echo "  ISUP TCP:     $(echo $SERVER | cut -d@ -f2):7660"
