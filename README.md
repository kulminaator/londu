# londu
db replication utility

not yet in usable state


grand plan:
* use no binary extension to postgres to get cross version logical replication of table data.
* replication idea is similar to slony and londiste, changes are written to events table as records and handled from there.

shortcomings :  
- double vs bigdecimal precision is a bit cheating but it seems to work just fine, we use more than double precision in both pg and clojure.
- result of parallel transactions needs heavy regression testing.
- initial data sync is not super optimized yet.

## current state:
- initial copy seems to work
- simple replication seems to work
- i have tests!
- tests show that initial copy + simple replication chucks along just fine

## how to run tests
Copy `test/londu1/test_db_credentials.clj-example` file to `test/londu1/test_db_credentials.clj`, 
and correct the values in it so tests can access your local postgresql. 

Then execute 
`lein test`