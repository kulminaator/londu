#!/usr/bin/env bash

echo '-- creating databases '
psql template1 -f create-db.sql


echo '-- initializing databases '
export PGPASSFILE=test_pgpass
psql -h 127.0.0.1 londu1_test_source_db -f init-db.sql -U shopdb_user --no-password
psql -h 127.0.0.1 londu1_test_target_db -f init-db.sql -U shopdb_user --no-password

echo '-- putting some data into source'
psql -h 127.0.0.1 londu1_test_source_db -f create-worker-row.sql -U shopdb_user --no-password
psql -h 127.0.0.1 londu1_test_source_db -f create-item-row.sql -U shopdb_user --no-password
psql -h 127.0.0.1 londu1_test_source_db -f create-item-row.sql -U shopdb_user --no-password


echo '-- adding londu to databases '
export PGPASSFILE=test_pgpass
psql -h 127.0.0.1 londu1_test_source_db -f londu1-tables-and-triggers.sql -U shopdb_user --no-password
psql -h 127.0.0.1 londu1_test_target_db -f londu1-tables-and-triggers.sql -U shopdb_user --no-password
