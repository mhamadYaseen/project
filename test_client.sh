#!/bin/bash

# Simple test client for File Indexer Query Server
# Usage: ./test_client.sh [port]

PORT=${1:-8080}
HOST="localhost"

echo "🔗 Connecting to $HOST:$PORT..."
echo ""
echo "📝 Sending test commands..."
echo ""

(
  sleep 0.5
  echo "HELP"
  sleep 0.5
  echo "STATS"
  sleep 0.5
  echo "FIND name contains java"
  sleep 0.5
  echo "QUIT"
) | nc $HOST $PORT

echo ""
echo "✅ Test complete!"
