# Excel.Parse
create sql inserts from an excel table using custom types:
 - STRING
 
		for example: cell_value = value => at format - 'value'
 - DECIMAL
    
		for example: cell_value = 1 => at format - 1 (integer)
 - DATE
 
		for example: cell_value = 15.02.2018 => at format - '2018-02-15'
 - TIMESTAMP
 
		for example: cell_value = 15.02.2018 04:00:05 => at format - '2018-02-15 04:00:05'
 - TIME
 
		for example: cell_value = 04:00:05 => at format - '04:00:05'
 - FUNCTION
 
		for example: cell_value = mod(100,10) => at format - mod(100,10)
 - MONEY
 
		for example: cell_value = 100.00/100,00 => at format - '100.00'
