CREATE OR REPLACE LANGUAGE plpgsql;

-- separator --

CREATE SCHEMA IF NOT EXISTS __londu_1;
DROP TABLE IF EXISTS __londu_1.events ;

-- separator --

-- create events table
CREATE TABLE __londu_1.events (
  id BIGSERIAL PRIMARY KEY ,
  tick BIGINT, -- tick id
  tid BIGINT, -- transaction id
  s TEXT NOT NULL, -- target schema
  t TEXT NOT NULL, -- target table
  op TEXT NOT NULL, -- operation
  od TEXT, -- old data
  nd TEXT  -- new data
);


-- separator --

-- create the status table on the target, this is where we write down 'where we are' when we sync data over
DROP TABLE IF EXISTS __londu_1.states;
CREATE TABLE __londu_1.states(
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT
);

-- separator --

CREATE OR REPLACE FUNCTION __londu_1.latest_tick() RETURNS bigint AS $$
DECLARE
  current_tick bigint := NULL;
BEGIN
  -- this lock will be held until our "outer most" transaction commits or rolls back.
  SELECT ltm.id INTO current_tick FROM __londu_1.ticks ltm
    WHERE id=(SELECT MAX(id) FROM __londu_1.ticks lts WHERE lts.tid < txid_current()) FOR SHARE;
  RETURN current_tick;
END;
$$ LANGUAGE plpgsql;

-- separator --

-- create trigger func
CREATE OR REPLACE FUNCTION __londu_1.trigger() RETURNS trigger AS $londu_1_trigger_function$
DECLARE
  old_data json := NULL;
  new_data json := NULL;
  current_tick bigint := NULL;
BEGIN
  IF TG_OP IN ('DELETE', 'UPDATE') THEN
    old_data = to_json(OLD);
  END IF;
  IF TG_OP IN ('INSERT', 'UPDATE') THEN
    new_data = to_json(NEW);
  END IF;
  current_tick := __londu_1.latest_tick();
  INSERT INTO __londu_1.events (tid, tick, s, t, op, od, nd)
    VALUES (txid_current(), current_tick, TG_TABLE_SCHEMA, TG_TABLE_NAME , TG_OP, old_data, new_data);
  IF TG_OP IN ('INSERT', 'UPDATE') THEN
    RETURN NEW;
  END IF;
  IF TG_OP IN ('DELETE') THEN
    RETURN OLD;
  END IF;
END;
$londu_1_trigger_function$ LANGUAGE plpgsql;

-- separator --

-- create ticks table
DROP TABLE IF EXISTS __londu_1.ticks;
CREATE TABLE __londu_1.ticks (
  id BIGSERIAL PRIMARY KEY ,
  tid BIGINT DEFAULT txid_current(),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- separator --

-- create the first tick
INSERT INTO __londu_1.ticks(created_at) VALUES(default);