#!/usr/bin/env bash
# wait-for-postgres.sh - Wait for PostgreSQL to be ready

set -e

host="$1"
shift

until PGPASSWORD=$SPRING_DATASOURCE_PASSWORD psql -h "$host" -U "$SPRING_DATASOURCE_USERNAME" -d postgres -c '\q' 2>/dev/null; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 2
done

>&2 echo "Postgres is up - executing command"
exec "$@"