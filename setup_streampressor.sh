#!/bin/bash

echo "=== StreamPressor Setup Script (Chisel 7.6.0) ==="
echo "This script will install all dependencies and configure the environment."
echo ""

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$SCRIPT_DIR"

# Check if we're in the project root (look for build.sbt)
if [ ! -f "$PROJECT_DIR/build.sbt" ]; then
    echo "Warning: build.sbt not found in $PROJECT_DIR"
    echo "Please run this script from the StreamPressor project root directory."
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Update system
echo "1. Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Java (11 and 17 for compatibility)
echo "2. Installing Java 11 and 17..."
sudo apt install -y openjdk-11-jdk openjdk-17-jdk

# Set JAVA_HOME
echo "2a. Setting JAVA_HOME..."
if [ -z "$JAVA_HOME" ]; then
    # Try to find Java 17 first, then Java 11
    if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    elif [ -d "/usr/lib/jvm/java-11-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
    else
        # Fallback: try to detect from java command
        JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
        export JAVA_HOME
    fi
    echo "JAVA_HOME set to: $JAVA_HOME"
    
    # Add to ~/.bashrc for persistence
    if ! grep -q "JAVA_HOME" ~/.bashrc 2>/dev/null; then
        echo "" >> ~/.bashrc
        echo "# Java Home" >> ~/.bashrc
        echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
        echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
    fi
else
    echo "JAVA_HOME already set to: $JAVA_HOME"
fi

# Install sbt
echo "3. Installing sbt (Scala Build Tool)..."
if [ ! -f /etc/apt/sources.list.d/sbt.list ]; then
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add -
    sudo apt update
fi
sudo apt install -y sbt

# Install Verilator and Z3
echo "4. Installing Verilator and Z3..."
sudo apt install -y verilator z3

# Install Python and dependencies
echo "5. Installing Python and dependencies..."
sudo apt install -y python3 python3-dev python3-pip python3-numpy libpython3-dev

# Detect Python version and fix library linking
echo "6. Fixing Python library linking..."
PYTHON_VERSION=$(python3 --version 2>&1 | grep -oP '\d+\.\d+' | head -1)
echo "Detected Python version: $PYTHON_VERSION"

# Find Python library
PYTHON_LIB=$(find /usr/lib/x86_64-linux-gnu -name "libpython${PYTHON_VERSION}.so" 2>/dev/null | head -1)
if [ -z "$PYTHON_LIB" ]; then
    # Try alternative locations
    PYTHON_LIB=$(find /usr/lib -name "libpython${PYTHON_VERSION}.so" 2>/dev/null | head -1)
fi

if [ -n "$PYTHON_LIB" ]; then
    echo "Found Python library: $PYTHON_LIB"
    sudo ln -sf "$PYTHON_LIB" /usr/lib/x86_64-linux-gnu/libpython3.so || true
    # Also try to create .so.1 link if it exists
    PYTHON_LIB_1="${PYTHON_LIB}.1"
    if [ -f "$PYTHON_LIB_1" ]; then
        sudo ln -sf "$PYTHON_LIB_1" /usr/lib/x86_64-linux-gnu/libpython3.so.1 || true
    fi
else
    echo "Warning: Could not find Python library. You may need to manually link it."
fi

# Set library path
export LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:/usr/lib:$LD_LIBRARY_PATH

# Navigate to project directory
echo "7. Setting up project..."
cd "$PROJECT_DIR"
echo "Working directory: $(pwd)"

# Verify build.sbt exists
if [ ! -f "build.sbt" ]; then
    echo "Error: build.sbt not found in $(pwd)"
    exit 1
fi

# Clean and compile
echo "8. Building project with Chisel 7.6.0..."
# Ensure JAVA_HOME is set for sbt
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    elif [ -d "/usr/lib/jvm/java-11-openjdk-amd64" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
    fi
fi
export PATH=$JAVA_HOME/bin:$PATH
sbt clean
sbt compile

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Compilation successful!"
    echo ""
    
    # Ask if user wants to run tests
    read -p "9. Run tests now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Running tests..."
        sbt test
    else
        echo "Skipping tests. You can run them later with: sbt test"
    fi
else
    echo ""
    echo "✗ Compilation failed. Please check the errors above."
    echo "Note: If you see import errors related to 'chisel3', you may need to update"
    echo "      import statements from 'chisel3' to 'chisel' for Chisel 7.6.0 compatibility."
    exit 1
fi

echo ""
echo "=== Setup Complete! ==="
echo "Project directory: $PROJECT_DIR"
echo "Chisel version: 7.6.0"
echo ""
echo "If you see test failures related to Python, the core functionality is working."
echo "The Python integration tests may need additional configuration."
echo ""
echo "To test backward compatibility, try: sbt compile"
echo "If compilation fails with import errors, update 'chisel3' imports to 'chisel'" 
