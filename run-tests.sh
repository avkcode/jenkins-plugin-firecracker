#!/bin/bash
set -e

# Build the plugin
mvn clean package

# Start the testing environment
docker-compose up -d

# Wait for Jenkins to start
echo "Waiting for Jenkins to start..."
until $(curl --output /dev/null --silent --head --fail http://localhost:8080); do
    printf '.'
    sleep 5
done
echo "Jenkins is up and running!"

# Run tests here
# ...

# Cleanup
echo "Press Ctrl+C to stop the testing environment"
read -r -d '' _ </dev/tty

docker-compose down
