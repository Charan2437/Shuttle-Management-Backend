#!/bin/bash

# Redis Setup Script for Shuttle System Caching
# This script helps install and configure Redis for the caching system

echo "🚀 Setting up Redis for Shuttle System Caching..."

# Detect operating system
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "📦 Detected Linux system"
    
    # Check if Redis is already installed
    if command -v redis-server &> /dev/null; then
        echo "✅ Redis is already installed"
    else
        echo "📥 Installing Redis..."
        sudo apt-get update
        sudo apt-get install -y redis-server
    fi
    
    # Start Redis service
    echo "🔄 Starting Redis service..."
    sudo systemctl start redis-server
    sudo systemctl enable redis-server
    
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "📦 Detected macOS system"
    
    # Check if Homebrew is installed
    if ! command -v brew &> /dev/null; then
        echo "❌ Homebrew is not installed. Please install Homebrew first:"
        echo "   /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi
    
    # Check if Redis is already installed
    if command -v redis-server &> /dev/null; then
        echo "✅ Redis is already installed"
    else
        echo "📥 Installing Redis..."
        brew install redis
    fi
    
    # Start Redis service
    echo "🔄 Starting Redis service..."
    brew services start redis
    
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash, Cygwin)
    echo "📦 Detected Windows system"
    echo "⚠️  For Windows, please use one of the following options:"
    echo "   1. Use WSL (Windows Subsystem for Linux)"
    echo "   2. Use Docker: docker run -d -p 6379:6379 redis:latest"
    echo "   3. Download Redis for Windows from: https://github.com/microsoftarchive/redis/releases"
    echo ""
    echo "   If using Docker, run:"
    echo "   docker run -d -p 6379:6379 redis:latest"
    exit 0
else
    echo "❌ Unsupported operating system: $OSTYPE"
    exit 1
fi

# Test Redis connection
echo "🧪 Testing Redis connection..."
if redis-cli ping | grep -q "PONG"; then
    echo "✅ Redis is running successfully!"
else
    echo "❌ Redis connection failed. Please check Redis installation."
    exit 1
fi

# Configure Redis for better performance (optional)
echo "⚙️  Configuring Redis for better performance..."
redis-cli config set maxmemory-policy allkeys-lru
redis-cli config set save ""

# Test cache functionality
echo "🧪 Testing cache functionality..."
redis-cli set "shuttle-system:test" "hello" EX 60
if redis-cli get "shuttle-system:test" | grep -q "hello"; then
    echo "✅ Cache functionality test passed!"
    redis-cli del "shuttle-system:test"
else
    echo "❌ Cache functionality test failed!"
    exit 1
fi

echo ""
echo "🎉 Redis setup completed successfully!"
echo ""
echo "📋 Next steps:"
echo "   1. Start your Spring Boot application"
echo "   2. Monitor cache usage with: redis-cli MONITOR"
echo "   3. Check cache keys with: redis-cli KEYS shuttle-system:*"
echo ""
echo "🔧 Useful Redis commands:"
echo "   - Check Redis status: redis-cli ping"
echo "   - Monitor operations: redis-cli MONITOR"
echo "   - List all cache keys: redis-cli KEYS shuttle-system:*"
echo "   - Check memory usage: redis-cli info memory"
echo "   - Flush all data: redis-cli FLUSHALL"
echo ""
echo "📚 For more information, see: CACHE_IMPLEMENTATION.md" 