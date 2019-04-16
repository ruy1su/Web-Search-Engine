#!/bin/bash
queries=( "information%20retrieval" "the%20matrix" "algebra" "elasticity" "elizabeth" "April%205" "wrestler" )
TMP_DIR=/tmp/p1grading/
REQUIRED_FILES="GetWebpage.java ParseJSON.java build.sh task3.txt"
ZIP_FILE=$1

task2a=' "count" : 502,
  "count" : 69555,
  "count" : 55,
  "count" : 9,
  "count" : 202,
  "count" : 3487,
  "count" : 85,'

task2b=' "count" : 343,
  "count" : 62493,
  "count" : 26,
  "count" : 3,
  "count" : 0,
  "count" : 2635,
  "count" : 21,'

task2c=' "count" : 788,
  "count" : 25,
  "count" : 74,
  "count" : 23,
  "count" : 203,
  "count" : 3487,
  "count" : 111,'

function error_exit()
{
   echo "ERROR: $1" 1>&2
   rm -rf ${TMP_DIR}
   exit 1
}

# retrieve a file from a URL (first argument) and check its SHA1 (second argument)
# and save it in a directory (third argument) 
function retrieve_file()
{
    URL=$1
    SHA1=$2
    RETRIEVE_DIR=$3
    FILENAME=$(basename ${URL})
    if [ -z ${RETRIEVE_DIR} ]; then
        TESTFILE="${FILENAME}"
    else
        mkdir -p ${RETRIEVE_DIR}
        TESTFILE="${RETRIEVE_DIR}/${FILENAME}"
    fi

    # check whether the file has already been retrieved and exists
    if [ -f ${TESTFILE} ]; then
        sha1sum ${TESTFILE} | grep ${SHA1} &> /dev/null
        if [ $? -eq 0 ]; then
            # the file already exists and its checksum matches
            return
        fi
    fi

    # the file does not exist. retrieve the file
    curl -s ${URL} > ${TESTFILE}
    if [ $? -ne 0 ]; then
        error_exit "Failed to retrieve ${FILENAME} file"
    fi
    sha1sum ${TESTFILE} | grep ${SHA1} &> /dev/null
    if [ $? -ne 0 ]; then
        error_exit "Failed to retrieve ${FILENAME} file. Checksum mismatch."
    fi
}

# usage
if [ $# -ne 1 ]
then
     echo "Usage: $0 project1.zip" 1>&2
     exit
fi

if [ `hostname` != "cs246" ]; then
     echo "You need to run this script within the class virtual machine" 1>&2
     exit
fi

curl -s -XPOST 'localhost:9200/?pretty' > /dev/null
if [ $? -ne 0 ]; then
     echo "Cannot connect to the Elasticsearch server. Have you started it?"
     exit
fi

# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# unzip the submission zip file 
if [ ! -f ${ZIP_FILE} ]; then
    error_exit "Cannot find $ZIP_FILE"
fi
unzip -q -d ${TMP_DIR} ${ZIP_FILE}
if [ "$?" -ne "0" ]; then 
    error_exit "Cannot unzip ${ZIP_FILE} to ${TMP_DIR}"
fi

# change directory to the grading folder
cd ${TMP_DIR}

# check the existence of the required files
for FILE in ${REQUIRED_FILES}
do
    if [ ! -f ${FILE} ]; then
        error_exit "Cannot find ${FILE} in the root folder of your zip file"
    fi
done

echo "Compiling GetWebpage.java..."

javac GetWebpage.java
if [ "$?" -ne "0" ] 
then
    error_exit "Compilation of GetWebpage.java failed"
fi

echo "Testing GetWebpage.java..."
java GetWebpage http://stackoverflow.com > result1.txt
curl -s http://stackoverflow.com > result2.txt

diff -w result1.txt result2.txt > /dev/null

if [ $? -eq 0 ]
then
    echo "SUCCESS!" 1>&2
else
    error_exit "GetWebpage didn't print the right page."
fi

# retrieve project1.zip file and extract simplewiki-abstract.json from the file
retrieve_file "http://oak.cs.ucla.edu/classes/cs246/projects/project1.zip" 5355e9e2f0b323b0e4154e3062b2fdfaf2896a8e "/tmp/retrieve-cache/"
unzip -q "/tmp/retrieve-cache/project1.zip" data/simplewiki-abstract.json

echo
echo "Running build.sh..."
curl -s -XDELETE 'localhost:9200/*?pretty' > /dev/null
chmod a+rx ./build.sh
./build.sh &> /dev/null

curl -s -XPOST 'localhost:9200/_refresh?pretty' > /dev/null

echo
echo "Testing Task2A..."
for query in "${queries[@]}"
do
    curl -s -XGET "localhost:9200/task2a/_count?q=$query&pretty" | grep count >> task2a.txt
done

diff -w task2a.txt <(echo "$task2a") > /dev/null
if [ $? -eq 0 ]
then
    echo "SUCCESS!" 1>&2
else
    error_exit "Query results from task2a incorrect."
fi


echo
echo "Testing Task2B..."
for query in "${queries[@]}"
do
    curl -s -XGET "localhost:9200/task2b/_count?q=$query&pretty" | grep count >> task2b.txt
done

diff -w task2b.txt <(echo "$task2b") > /dev/null
if [ $? -eq 0 ]
then
    echo "SUCCESS!" 1>&2
else
    error_exit "Query results from task2b incorrect."
fi

echo
echo "Testing Task2C..."
for query in "${queries[@]}"
do
    curl -s -XGET "localhost:9200/task2c/_count?q=$query&pretty" | grep count >> task2c.txt
done

diff -w task2c.txt <(echo "$task2c") > /dev/null
if [ $? -eq 0 ]
then
    echo "SUCCESS!" 1>&2
else
    error_exit "Query results from task2c incorrect."
fi

# clean up
rm -rf ${TMP_DIR}
exit 0