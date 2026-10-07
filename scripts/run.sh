#!/usr/bin/env bash
#
# run.sh -- build the runnable jar and run it with pass-through arguments.
#
# Works from any directory: it resolves the repository root from this script's
# location, builds target/thread-pooling.jar (tests skipped for a fast run), and
# then runs it, forwarding every argument to the program. Examples:
#   scripts/run.sh                     # compare mode at the default N
#   scripts/run.sh --n 1000000 --mode pool
#   scripts/run.sh --mode single --output primes.txt
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"
mvn -q -DskipTests package
java -jar target/thread-pooling.jar "$@"
