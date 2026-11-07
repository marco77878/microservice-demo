#!/bin/bash

echo "Stopping local microservices..."

if [ -f .pids ]; then
    while IFS= read -r pid; do
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            echo "Terminated PID: $pid"
        fi
    done < .pids
    rm -f .pids
fi

# Fallback: kill any leftover process on ports 7573, 8761, 8888, 8081, 8082
for port in 7573 8761 8888 8081 8082; do
    pid=$(lsof -ti :$port 2>/dev/null)
    if [ -n "$pid" ]; then
        kill -9 $pid 2>/dev/null
        echo "Freed port $port (PID: $pid)"
    fi
done

echo "All microservices stopped successfully."
