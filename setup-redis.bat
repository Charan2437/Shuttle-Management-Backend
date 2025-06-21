@echo off
echo 🚀 Setting up Redis for Shuttle System Caching...

REM Check if Docker is available
docker --version >nul 2>&1
if %errorlevel% == 0 (
    echo 📦 Docker detected. Starting Redis container...
    docker run -d -p 6379:6379 --name shuttle-redis redis:latest
    if %errorlevel% == 0 (
        echo ✅ Redis container started successfully!
        goto :test_redis
    ) else (
        echo ❌ Failed to start Redis container
        goto :manual_setup
    )
) else (
    echo ⚠️  Docker not found
    goto :manual_setup
)

:manual_setup
echo.
echo 📋 Manual Redis Setup Options:
echo.
echo 1. Install WSL (Windows Subsystem for Linux):
echo    - Open PowerShell as Administrator
echo    - Run: wsl --install
echo    - Restart your computer
echo    - In WSL, run: sudo apt-get install redis-server
echo.
echo 2. Download Redis for Windows:
echo    - Visit: https://github.com/microsoftarchive/redis/releases
echo    - Download the latest release
echo    - Extract and run redis-server.exe
echo.
echo 3. Use Docker Desktop:
echo    - Download from: https://www.docker.com/products/docker-desktop
echo    - Install and start Docker Desktop
echo    - Run: docker run -d -p 6379:6379 redis:latest
echo.
goto :end

:test_redis
echo.
echo 🧪 Testing Redis connection...
timeout /t 3 /nobreak >nul

REM Try to connect to Redis
echo ping | redis-cli >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ Redis is running successfully!
    
    echo.
    echo 🧪 Testing cache functionality...
    echo set shuttle-system:test hello | redis-cli >nul 2>&1
    echo expire shuttle-system:test 60 | redis-cli >nul 2>&1
    
    echo get shuttle-system:test | redis-cli | findstr "hello" >nul 2>&1
    if %errorlevel% == 0 (
        echo ✅ Cache functionality test passed!
        echo del shuttle-system:test | redis-cli >nul 2>&1
    ) else (
        echo ❌ Cache functionality test failed!
    )
) else (
    echo ❌ Redis connection failed. Please check Redis installation.
    goto :manual_setup
)

:end
echo.
echo 🎉 Redis setup completed!
echo.
echo 📋 Next steps:
echo    1. Start your Spring Boot application
echo    2. Monitor cache usage with: redis-cli MONITOR
echo    3. Check cache keys with: redis-cli KEYS shuttle-system:*
echo.
echo 🔧 Useful Redis commands:
echo    - Check Redis status: redis-cli ping
echo    - Monitor operations: redis-cli MONITOR
echo    - List all cache keys: redis-cli KEYS shuttle-system:*
echo    - Check memory usage: redis-cli info memory
echo    - Flush all data: redis-cli FLUSHALL
echo.
echo 📚 For more information, see: CACHE_IMPLEMENTATION.md
echo.
pause 