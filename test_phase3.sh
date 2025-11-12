#!/bin/bash

# Interactive Test Script for Phase 3 TCP Server
# This demonstrates all the commands you can use

echo "=================================="
echo "📁 File Indexer - Phase 3 Testing"
echo "=================================="
echo ""
echo "Server is running on localhost:8080"
echo ""
echo "🎯 This script will connect and run example commands:"
echo ""

# Connect and send commands
(
  echo "  → Sending HELP command..."
  echo "HELP"
  sleep 1.5
  
  echo ""
  echo "  → Sending STATS command..."
  echo "STATS"
  sleep 1.5
  
  echo ""
  echo "  → Searching for files containing 'Database'..."
  echo "FIND name contains Database"
  sleep 1.5
  
  echo ""
  echo "  → Searching for .java files..."
  echo "FIND ext is java"
  sleep 1.5
  
  echo ""
  echo "  → Searching for files larger than 10 KB..."
  echo "FIND size > 10240"
  sleep 1.5
  
  echo ""
  echo "  → Disconnecting..."
  echo "QUIT"
  
) | nc localhost 8080

echo ""
echo "=================================="
echo "✅ Test Complete!"
echo "=================================="
echo ""
echo "💡 You can also connect manually with:"
echo "   telnet localhost 8080"
echo "   or"
echo "   nc localhost 8080"
echo ""
