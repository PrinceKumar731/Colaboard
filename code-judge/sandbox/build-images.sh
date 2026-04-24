#!/usr/bin/env bash
# Build all sandbox Docker images required by the judge engine.
# Run this once before starting the backend.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Building judge-java:latest …"
docker build -t judge-java:latest   "$SCRIPT_DIR/java"

echo "Building judge-python:latest …"
docker build -t judge-python:latest "$SCRIPT_DIR/python"

echo "Building judge-cpp:latest …"
docker build -t judge-cpp:latest    "$SCRIPT_DIR/cpp"

echo ""
echo "All sandbox images built successfully."
docker images | grep "judge-"
