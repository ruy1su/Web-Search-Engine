#!/bin/bash

INDEX=$1
filecount=0
precision=0

#get the query and expected results

while IFS='' read -r line || [[ -n "$line" ]]; do
    IFS=' : ' read -r QUERY results <<< "$line"
    read -r -a expected <<< "$results"
    filecount=`echo "$filecount" | awk '{printf "%.2f \n", $1+1}'`
    # filecount=$(echo "scale=2 ; $filecount + 1 " | bc)

#run the query
	curl -s -XGET "localhost:9200/$INDEX/wikipage/_search?q=$QUERY&pretty" > result.txt 2> /dev/null


#grep actual result for expected result and calculate precision@10
	i=0

	for expect in "${expected[@]}"
	do 
		grep $expect result.txt > /dev/null
		if [ $? -eq 0 ]
		then i=$(($i+1));
		fi
	done

#write precision@10
	p=`echo "$i" | awk '{printf "%.2f \n", $1/10}'`
	# p=$(echo "scale=2 ; $i / 10" | bc)
	
# uncomment the next line to see indiviual p@10 values
	#echo "$QUERY : $p"


	precision=`echo "$precision $p" | awk '{printf "%.2f \n", $1+$2}'`
	# precision=$(echo "scale=2 ; $precision + $p" | bc)
done < data/benchmark.txt

#calculate average precision@10 and print 
average=`echo "$precision $filecount" | awk '{printf "%.4f \n", $1/$2}'`
# average=$(echo "scale=5 ; $precision / $filecount" | bc)
echo "Average P@10: $average"
rm result.txt
