-- creating the test target database here
DROP USER IF EXISTS londu_1_replicator;
-- separator --
DROP USER IF EXISTS shopdb_target_user;
-- separator --
-- create all
CREATE USER londu_1_replicator PASSWORD 'londu_1_replicator';
-- separator --
CREATE USER shopdb_target_user SUPERUSER PASSWORD 'shopdb_target_user';
-- separator --
CREATE DATABASE londu1_test_target_db OWNER shopdb_target_user;