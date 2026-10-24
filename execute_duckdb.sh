#!/usr/bin/env bash

set -euo pipefail
mkdir -p results_duckdb

for file in input/queries/*.sql; do
  name=$(basename "$file" .sql)

  ./duckdb input/db/test.db -csv -header <"$file" >"results_duckdb/${name}.csv"
done
