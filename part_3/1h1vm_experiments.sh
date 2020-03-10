#!/bin/bash

for i in {1..5}
do
	command time -ao 1h1vm_each_times.txt hadoop jar ~/code/org/myorg/MaxReduceJob.jar MaxTemperature data.txt 1ph1vm$i
done
