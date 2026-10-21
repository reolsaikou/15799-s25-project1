#!/usr/bin/env bash

set -euo pipefail

QUERIES=$1
OUTPUT_DIR=$2
DB_PATH=$3

echo "Invoking calcite.sh."

echo -e "\tQueries: ${QUERIES}"
echo -e "\tOutput Dir: ${OUTPUT_DIR}"
echo -e "\tDB Path: ${DB_PATH}"

cd calcite_app/
./gradlew build
./gradlew shadowJar
./gradlew --stop
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:5005 -Xmx4096m -Dorg.slf4j.simpleLogger.log.org.apache.calcite.sql.timing=debug -jar calcite_app/build/libs/calcite_app-1.0-SNAPSHOT-all.jar input/queries results input/db/test.db
