-- run this to bootstrap a database structure

CREATE TABLE shop_workers (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL
);

-- separator --
CREATE TABLE shop_items (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  price NUMERIC(20,6)
);
-- separator --

-- access to tables to replicator
GRANT INSERT, UPDATE, SELECT, TRUNCATE ON TABLE shop_workers TO londu_1_replicator;
-- separator --
GRANT INSERT, UPDATE, SELECT, TRUNCATE ON TABLE shop_items TO londu_1_replicator;