#!/bin/bash

# Java environment configuration
export JAVA_HOME=${JAVA_HOME:-/usr/local/opt/openjdk}
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p logs

echo "=================================================="
echo "Starting local microservices (Non-Docker Mode)..."
echo "=================================================="

# 1. Discovery Service (Eureka)
echo "[1/5] Starting Eureka Discovery Service (port: 8761)..."
nohup java -jar discovery-service/target/discovery-service-1.0.0.jar > logs/discovery.log 2>&1 &
EUREKA_PID=$!
echo $EUREKA_PID > .pids
sleep 6

# 2. Config Service
echo "[2/5] Starting Config Service (port: 8888)..."
nohup java -jar config-service/target/config-service-1.0.0.jar > logs/config.log 2>&1 &
echo $! >> .pids
sleep 4

# 3. UAA Service
echo "[3/5] Starting UAA Authentication Service (port: 8081)..."
nohup java -jar uaa-service/target/uaa-service-1.0.0.jar > logs/uaa.log 2>&1 &
echo $! >> .pids

# 4. Product Service
echo "[4/5] Starting Product Service (port: 8082)..."
nohup java -jar product-service/target/product-service-1.0.0.jar > logs/product.log 2>&1 &
echo $! >> .pids
sleep 6

# 5. Gateway Service (Port 7573 exposed)
echo "[5/5] Starting Gateway Service (port: 7573)..."
nohup java -jar gateway-service/target/gateway-service-1.0.0.jar > logs/gateway.log 2>&1 &
echo $! >> .pids

echo "=================================================="
echo "All microservices started in background."
echo "Login URL: http://localhost:7573/login"
echo "Log files are stored in logs/ directory"
echo "To stop services, run: ./stop-local.sh"
echo "=================================================="
