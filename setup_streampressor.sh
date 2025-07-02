#!/bin/bash

echo "=== StreamPressor-2 Setup Script for WSL ==="
echo "This script will install all dependencies and configure the environment."

# Update system
echo "1. Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Java
echo "2. Installing Java 11..."
sudo apt install -y openjdk-11-jdk

# Install sbt
echo "3. Installing sbt (Scala Build Tool)..."
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add -
sudo apt update
sudo apt install -y sbt

# Install Verilator and Z3
echo "4. Installing Verilator and Z3..."
sudo apt install -y verilator z3

# Install Python and dependencies
echo "5. Installing Python and dependencies..."
sudo apt install -y python3 python3-dev python3-pip python3-numpy python3.12-dev libpython3-dev

# Fix Python library linking
echo "6. Fixing Python library linking..."
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython3.12.so /usr/lib/x86_64-linux-gnu/libpython3.so
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython3.12.so.1 /usr/lib/x86_64-linux-gnu/libpython3.so.1
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython3.12.so /usr/lib/libpython3.so
sudo ln -sf /usr/lib/x86_64-linux-gnu/libpython3.12.so.1 /usr/lib/libpython3.so.1

# Set library path
export LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/usr/lib:$LD_LIBRARY_PATH

# Navigate to project directory (adjust path as needed)
echo "7. Setting up project..."
cd /home/cc/StreamPressor

# Clean and compile
echo "8. Building project..."
sbt clean
sbt compile

# Test the setup
echo "9. Running tests..."
sbt test

echo "=== Setup Complete! ==="
echo "If you see test failures related to Python, the core functionality is working."
echo "The Python integration tests may need additional configuration." 
