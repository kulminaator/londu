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