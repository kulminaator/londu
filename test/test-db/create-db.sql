-- run this to bootstrap the env
DROP DATABASE IF EXISTS londu1_test_source_db;
DROP DATABASE IF EXISTS londu1_test_target_db;
DROP USER IF EXISTS londu_1_replicator;
DROP USER IF EXISTS shopdb_user;

-- create
CREATE USER londu_1_replicator PASSWORD 'londu_1_replicator';
CREATE USER shopdb_user SUPERUSER PASSWORD 'shopdb_user';
CREATE DATABASE londu1_test_source_db OWNER shopdb_user;
CREATE DATABASE londu1_test_target_db OWNER shopdb_user;