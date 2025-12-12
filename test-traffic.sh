#!/bin/bash
# Simulate traffic and show distribution
# Usage: ./test-traffic.sh [count] [url]

COUNT=${1:-100}
URL=${2:-https://abtesting-79wy.onrender.com}

echo "Simulating $COUNT users on $URL..."
echo ""

for i in $(seq 1 $COUNT); do
  COLOR=$(curl -s "$URL" -c /dev/null -b "UserId=user_$i" | grep -o 'background-color: [^;]*' | head -1 | cut -d' ' -f2)
  case $COLOR in
    "#ff6600") echo "orange" ;;
    "#CD29C0") echo "magenta" ;;
    "#2164f3") echo "blue" ;;
    "#008040") echo "green" ;;
    *) echo "white (inactive)" ;;
  esac
done | sort | uniq -c | sort -rn

echo ""
echo "Done."
