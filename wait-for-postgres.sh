#!/usr/bin/env bash
# wait-for-postgres.sh - Wait for PostgreSQL to be ready

set -e

host="$1"
shift

echo "Waiting for PostgreSQL at $host..."

# Wait for PostgreSQL to accept connections
until pg_isready -h "$host" -U postgres 2>/dev/null; do
  echo "Postgres is unavailable - sleeping"
  sleep 2
done

echo "Postgres is up - executing command"
exec "$@"