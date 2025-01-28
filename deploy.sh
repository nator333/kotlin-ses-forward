#!/usr/bin/env bash
#
# Deployment script for the Kotlin SES Forward application
# This script checks for required environment variables, runs code formatting,
# builds the application, and deploys it using the Serverless framework.

# Source .env file if it exists
if [ -f .env ]; then
    source .env
fi

# Required environment variables
required_vars=("MAIL_TO" "MAIL_FROM" "REGION_ID" "EVENT_BUCKET" "DEPLOYMENT_BUCKET")

# Check if all required environment variables are set
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "Error: ${var} environment variable is not defined or empty"
        exit 1
    fi
done

# Format code, build and deploy
./gradlew :app:spotlessApply
./gradlew :app:buildZip
serverless deploy