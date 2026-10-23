#!/usr/bin/env bash

set -euo pipefail

WORKLOAD=$1
OUTPUT_DIR=$2

echo "Invoking optimize.sh."
echo -e "\tWorkload: ${WORKLOAD}"
echo -e "\tOutput Dir: ${OUTPUT_DIR}"

mkdir -p "${OUTPUT_DIR}"
mkdir -p input/

# Extract the workload.
tar xzf "${WORKLOAD}" --directory input/

# Feel free to add more steps here.
#
# Create DuckDB database.
./duckdb ./"${OUTPUT_DIR}"/tpc-h.db <input/data/schema.sql

# Load Data
(
  cd input

  ../duckdb ../"${OUTPUT_DIR}"/tpc-h.db <data/load.sql
  cd ..
)

# Build and run the Calcite app.
cd calcite_app/
./gradlew build
./gradlew shadowJar
./gradlew --stop
java -Xmx4096m -jar build/libs/calcite_app-1.0-SNAPSHOT-all.jar "../input/queries" "../${OUTPUT_DIR}" "../${OUTPUT_DIR}/tpc-h.db"
cd -
