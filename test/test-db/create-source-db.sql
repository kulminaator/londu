-- create source test db
-- separator --
DROP USER IF EXISTS londu_1_replicator;
-- separator --
DROP USER IF EXISTS shopdb_source_user;
-- separator --

-- create all
CREATE USER londu_1_replicator PASSWORD 'londu_1_replicator';
-- separator --
CREATE USER shopdb_source_user SUPERUSER PASSWORD 'shopdb_source_user';
-- separator --
CREATE DATABASE londu1_test_source_db OWNER shopdb_source_user;