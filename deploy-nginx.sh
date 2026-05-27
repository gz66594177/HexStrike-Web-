#!/bin/bash
set -euo pipefail

# Configuration
DOMAIN="${DOMAIN:-localhost}"  # Default to localhost if not set; change for production
EMAIL="${EMAIL:-admin@example.com}"  # Change for production
STAGING="${STAGING:-false}"  # Set to true for testing with Let's Encrypt staging endpoint

# Paths
CERTBOT_CONF="./certbot/conf"
CERTBOT_WWW="./certbot/www"

# Function to initialize certificates
init_certificates() {
    echo "Initializing Let's Encrypt certificate for domain: $DOMAIN"
    
    # Create webroot directory if it doesn't exist
    mkdir -p "$CERTBOT_WWW"
    
    # Choose staging or production endpoint
    if [ "$STAGING" = true ]; then
        STAGING_FLAG="--staging"
        echo "Using Let's Encrypt staging endpoint"
    else
        STAGING_FLAG=""
    fi
    
    # Run certbot in standalone mode (temporarily uses port 80)
    # We'll stop nginx briefly if it's running, but in our setup we can use webroot
    # Since we have nginx running, we'll use the webroot plugin
    docker run --rm \
        -v "$(pwd)/$CERTBOT_CONF:/etc/letsencrypt" \
        -v "$(pwd)/$CERTBOT_WWW:/var/www/certbot" \
        certbot/certbot certonly \
        --webroot \
        -w /var/www/certbot \
        -d "$DOMAIN" \
        --email "$EMAIL" \
        --agree-tos \
        --no-eff-email \
        $STAGING_FLAG \
        --force-renewal
    
    echo "Certificate initialization complete."
}

# Function to renew certificates (to be called by cron)
renew_certificates() {
    echo "Renewing Let's Encrypt certificates..."
    docker run --rm \
        -v "$(pwd)/$CERTBOT_CONF:/etc/letsencrypt" \
        -v "$(pwd)/$CERTBOT_WWW:/var/www/certbot" \
        certbot/certbot renew \
        --webroot -w /var/www/certbot \
        --quiet
    echo "Certificate renewal check complete."
}

# Function to reload nginx after certificate renewal
reload_nginx() {
    echo "Reloading nginx configuration..."
    docker-compose exec nginx nginx -s reload
    echo "Nginx reloaded."
}

# Main script logic
case "${1:-}" in
    init)
        init_certificates
        ;;
    renew)
        renew_certificates
        reload_nginx
        ;;
    *)
        echo "Usage: $0 {init|renew}"
        echo "  init:   Obtain initial certificates"
        echo "  renew:  Renew certificates and reload nginx"
        exit 1
        ;;
esac