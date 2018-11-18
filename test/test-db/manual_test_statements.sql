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