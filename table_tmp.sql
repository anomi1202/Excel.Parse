insert into table1 (field_string, field_int, field_date, field_timestamp, field_time, field_function, field_decimal_to_string) values (NULL, 100000, '2018-01-01', '2017-01-01 04:05:30', '04:00:00', mod(1000, 10), '1000');
insert into table1 (field_string, field_int, field_date, field_timestamp, field_time, field_function, field_decimal_to_string) values (NULL, 200000, '2018-01-01', '2017-01-02 04:05:30', '05:00:00', mod(2000, 10), '2000');
insert into table1 (field_string, field_int, field_date, field_timestamp, field_time, field_function, field_decimal_to_string) values (NULL, 300000, '2018-01-01', '2017-01-03 04:05:30', '06:00:00', mod(3000, 10), '3000');
commit;



