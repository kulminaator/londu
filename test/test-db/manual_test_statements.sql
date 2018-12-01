-- these are just used during manual tests to trigger "things"

-- add a tick
INSERT INTO __londu_1_ticks(created_at) VALUES(default);



-- add item
insert into shop_items (name, price) values ('aga', 145);
insert into shop_items (name, price) values ('aga', 156);
insert into shop_items (name, price) values ('aga', 157);


select * from __londu_1_ticks;
select * from __londu_1_ticks where id = 3 for update;


explain SELECT ltm.id FROM __londu_1_ticks ltm
    WHERE id=(SELECT max(id) FROM __londu_1_ticks lts WHERE lts.tid < txid_current());
-- x
CREATE TRIGGER __londu_1_trigger_temptest BEFORE INSERT OR UPDATE OR DELETE ON temptest
  FOR EACH ROW EXECUTE PROCEDURE __londu_1_trigger();

-- on both master and slave
DROP TABLE IF EXISTS  add_test_subject CASCADE ;
CREATE TABLE add_test_subject (id integer, someval text);

-- on master
INSERT INTO add_test_subject SELECT g, 'random__data_'||g FROM generate_series(1,1000) g;


WITH __londu_old_data AS
(
  SELECT * FROM json_populate_record(null::"other_stuff",
  '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"02:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this is": {"a": "structure"}}}')
  LIMIT 1
), __londu_new_data AS
(
  SELECT * FROM json_populate_record(null::"other_stuff",
  '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"03:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this here is": {"a": "structure"}}}')
  LIMIT 1
)


UPDATE "other_stuff"
 SET
  id = __londu_new_data.id,
  created_at = __londu_new_data.created_at,
  lunchtime = __londu_new_data.lunchtime,
  updated_at = __londu_new_data.updated_at,
  metadata = __londu_new_data.metadata
 FROM
   (
    SELECT * FROM json_populate_record(null::"other_stuff",
      '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"02:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this is": {"a": "structure"}}}')
      LIMIT 1
   ) __londu_old_data,
   (
     SELECT * FROM json_populate_record(null::"other_stuff",
     '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"03:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this here is": {"a": "structure"}}}')
     LIMIT 1
   ) __londu_new_data
 WHERE
  "other_stuff".id = __londu_old_data.id AND
  "other_stuff".created_at = __londu_old_data.created_at AND
  "other_stuff".lunchtime = __londu_old_data.lunchtime AND
  "other_stuff".updated_at = __londu_old_data.updated_at AND
  "other_stuff".metadata = __londu_old_data.metadata;




UPDATE "other_stuff"
 SET
  (id, created_at, lunchtime, updated_at, metadata) = (SELECT * FROM json_populate_record(null::"other_stuff",
     '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"02:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"x this here is": {"a": "structure"}}}')
     )
 FROM
   (
    SELECT * FROM json_populate_record(null::"other_stuff",
      ' {"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"03:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this here is": {"a": "structure"}}}')
      LIMIT 1
   ) __londu_old_data
 WHERE
  "other_stuff".id = __londu_old_data.id AND
  "other_stuff".created_at = __londu_old_data.created_at AND
  "other_stuff".lunchtime = __londu_old_data.lunchtime AND
  "other_stuff".updated_at = __londu_old_data.updated_at AND
  "other_stuff".metadata = __londu_old_data.metadata;

explain
UPDATE "other_stuff"
 SET
  (id, created_at, lunchtime, updated_at, metadata) = (SELECT * FROM json_populate_record(null::"other_stuff",
     '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"02:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"x this here is": {"a": "structure"}}}')
     )
 WHERE
  ("other_stuff") = (SELECT json_populate_record(null::"other_stuff",
      ' {"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"03:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"this here is": {"a": "structure"}}}')
);


-- suitable for updates

UPDATE "other_stuff"
 SET
  (id, created_at, lunchtime, updated_at, metadata) = (SELECT * FROM json_populate_record(null::"other_stuff",
     '{"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","updated_at":"2018-11-30T20:01:17.715844","lunchtime":"02:00:00","metadata":{"x this here is": {"a": "structure"}}}')
     )
 FROM
   (
    SELECT * FROM json_populate_record(null::"other_stuff",
      ' {"id":1,"created_at":"2018-11-30T20:01:17.715844+02:00","lunchtime":"02:00:00","updated_at":"2018-11-30T20:01:17.715844","metadata":{"x this here is": {"a": "structure"}}}')
      LIMIT 1
   ) __londu_old_data
 WHERE
  "other_stuff".id = __londu_old_data.id AND
  "other_stuff".created_at = __londu_old_data.created_at AND
  ("other_stuff".lunchtime = __londu_old_data.lunchtime OR ("other_stuff".lunchtime is null AND __londu_old_data.lunchtime is null))AND
  "other_stuff".updated_at = __londu_old_data.updated_at AND
  "other_stuff".metadata = __londu_old_data.metadata;


  UPDATE "public"."shop_workers"
         SET ("id","name","nickname","retired","born_at") = (
           SELECT * FROM json_populate_record(null::"public"."shop_workers", '{"id":3,"name":"alice","nickname":null,"retired":true,"born_at":"1995-06-02T02:15:02+03:00"}'::json)
         )
         FROM (
           SELECT * FROM json_populate_record(null::"public"."shop_workers", '{"id":3,"name":"alice","nickname":null,"retired":true,"born_at":null}'::json))
           ) __londu_old_data
         WHERE ("__londu_old_data"."id" = "public"."shop_workers"."id" OR ("__londu_old_data"."id" IS NULL AND "public"."shop_workers"."id" IS NULL)) AND ("__londu_old_data"."name" = "public"."shop_workers"."name" OR ("__londu_old_data"."name" IS NULL AND "public"."shop_workers"."name" IS NULL)) AND ("__londu_old_data"."nickname" = "public"."shop_workers"."nickname" OR ("__londu_old_data"."nickname" IS NULL AND "public"."shop_workers"."nickname" IS NULL)) AND ("__londu_old_data"."retired" = "public"."shop_workers"."retired" OR ("__londu_old_data"."retired" IS NULL AND "public"."shop_workers"."retired" IS NULL)) AND ("__londu_old_data"."born_at" = "public"."shop_workers"."born_at" OR ("__londu_old_data"."born_at" IS NULL AND "public"."shop_workers"."born_at" IS NULL))
