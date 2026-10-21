#!/usr/bin/env bash

set -euo pipefail

DB="db/test.db"

echo "Invoking load_data.sh"

cd input/

echo -e "database : ${DB}"

../duckdb ./${DB} <./data/schema.sql
../duckdb ./${DB} <./data/load.sql
