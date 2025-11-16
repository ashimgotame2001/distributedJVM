#!/bin/bash
# build-all.sh
# A script to clean, compile, and install all modules for Distributed JVM project

set -euo pipefail  # Exit on error, unset variable, or failed pipe

PROJECT_ROOT=$(pwd)

echo "=============================="
echo "Cleaning and building all modules..."
echo "Project root: $PROJECT_ROOT"
echo "=============================="

# Function to build a module
build_module() {
    local module_name=$1
    echo "------------------------------"
    echo "Building $module_name module..."
    cd "$PROJECT_ROOT/$module_name"
    mvn clean install -U
    echo "$module_name module built successfully."
    echo "------------------------------"
}

# Build order matters due to dependencies
build_module "grpc"        # Generates gRPC classes
build_module "shared"      # Shared code used by others
build_module "core"        # Core logic
build_module "node-agent"  # Node agent module
build_module "client"      # Client module

echo "=============================="
echo "All modules built successfully!"
echo "=============================="
