#!/bin/bash
if [ $# -ne 1 ]; then
	echo $0: pass in name of hadoop output files
	exit 1
fi

for i in {1..5}
do
	command time -ao $1_times.txt yarn jar ~/temp/code/org/myorg/MaxReduceJob.jar MaxTemperature data.txt $1$i
done
