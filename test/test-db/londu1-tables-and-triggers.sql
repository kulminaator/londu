CREATE OR REPLACE LANGUAGE plpgsql;

DROP TABLE IF EXISTS __londu_1_events ;

-- create events table
CREATE TABLE __londu_1_events (
  id BIGSERIAL PRIMARY KEY ,
  tick BIGINT, -- tick id
  tid BIGINT, -- transaction id
  s TEXT NOT NULL, -- target schema
  t TEXT NOT NULL, -- target table
  op TEXT NOT NULL, -- operation
  od TEXT, -- old data
  nd TEXT  -- new data
);

-- create the status table on the target, this is where we write down 'where we are' when we sync data over
DROP TABLE IF EXISTS __londu_1_states;
CREATE TABLE __londu_1_states(
  id BIGSERIAL PRIMARY KEY,
  event_id BIGINT
);

-- create trigger func
CREATE OR REPLACE FUNCTION __londu_1_trigger() RETURNS trigger AS $londu_1_trigger_function$
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
  SELECT ltm.id INTO current_tick FROM __londu_1_ticks ltm
    WHERE id=(SELECT MAX(id) FROM __londu_1_ticks lts WHERE lts.tid < txid_current()) FOR SHARE;
  INSERT INTO __londu_1_events (tid, tick, s, t, op, od, nd)
    VALUES (txid_current(), current_tick, TG_TABLE_SCHEMA, TG_TABLE_NAME , TG_OP, old_data, new_data);
  IF TG_OP IN ('INSERT', 'UPDATE') THEN
    RETURN NEW;
  END IF;
  IF TG_OP IN ('DELETE') THEN
    RETURN OLD;
  END IF;
END;
$londu_1_trigger_function$ LANGUAGE plpgsql;


-- recreate triggers
DROP TRIGGER IF EXISTS __londu_1_trigger_shop_workers ON shop_workers;
CREATE TRIGGER __londu_1_trigger_shop_workers BEFORE INSERT OR UPDATE OR DELETE ON shop_workers
  FOR EACH ROW EXECUTE PROCEDURE __londu_1_trigger();

DROP TRIGGER IF EXISTS __londu_1_trigger_shop_items ON shop_items;
CREATE TRIGGER __londu_1_trigger_shop_items BEFORE INSERT OR UPDATE OR DELETE ON shop_items
  FOR EACH ROW EXECUTE PROCEDURE __londu_1_trigger();

-- create ticks table
DROP TABLE IF EXISTS __londu_1_ticks;
CREATE TABLE __londu_1_ticks (
  id BIGSERIAL PRIMARY KEY ,
  tid BIGINT DEFAULT txid_current(),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO __londu_1_ticks(created_at) VALUES(default);