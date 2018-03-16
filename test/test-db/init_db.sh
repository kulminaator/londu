#!/usr/bin/env bash

export DB_PORT=5432


echo -n "Enter db port to initialize (default 5432)"
read TEST_DB_PORT
if [ "$TEST_DB_PORT" != "" ]; then
 export DB_PORT=$TEST_DB_PORT;
fi;

echo "Initializaing on db port $DB_PORT"

echo '-- creating databases '
## skipping host name here to allow local auth
psql -p $DB_PORT template1 -f create-db.sql


echo '-- initializing databases '
export PGPASSFILE=test_pgpass
psql -h 127.0.0.1 -p $DB_PORT londu1_test_source_db -f init-db.sql -U shopdb_user --no-password
psql -h 127.0.0.1 -p $DB_PORT londu1_test_target_db -f init-db.sql -U shopdb_user --no-password

echo '-- putting some data into source'
psql -h 127.0.0.1 -p $DB_PORT londu1_test_source_db -f create-worker-row.sql -U shopdb_user --no-password
psql -h 127.0.0.1 -p $DB_PORT londu1_test_source_db -f create-item-row.sql -U shopdb_user --no-password
psql -h 127.0.0.1 -p $DB_PORT londu1_test_source_db -f create-item-row.sql -U shopdb_user --no-password


echo '-- adding londu to databases '
export PGPASSFILE=test_pgpass
psql -h 127.0.0.1 -p $DB_PORT londu1_test_source_db -f londu1-tables-and-triggers.sql -U shopdb_user --no-password
psql -h 127.0.0.1 -p $DB_PORT londu1_test_target_db -f londu1-tables-and-triggers.sql -U shopdb_user --no-password
