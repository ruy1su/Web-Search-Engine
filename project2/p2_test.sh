#!/bin/bash

queries=( "information%20retrieval" "the%20matrix" "algebra" "elasticity" "elizabeth" )
TMP_DIR=/tmp/project2/
REQUIRED_FILES="build.sh ParseJSON.java build.gradle install-plugin.sh task3a.sh"
ZIP_FILE=$1

task3a='"max_score" : 89.30481,
        "_score" : 89.30481,
        "_score" : 86.686066,
        "_score" : 86.29083,
        "_score" : 86.09051,
        "_score" : 85.67467,
        "_score" : 85.61694,
        "_score" : 84.22537,
        "_score" : 83.28606,
        "_score" : 82.99002,
        "_score" : 80.8033,
    "max_score" : 40.151104,
        "_score" : 40.151104,
        "_score" : 38.99555,
        "_score" : 38.77317,
        "_score" : 38.37591,
        "_score" : 36.38686,
        "_score" : 36.263447,
        "_score" : 36.263447,
        "_score" : 36.249115,
        "_score" : 36.196007,
        "_score" : 36.13217,
    "max_score" : 91.52402,
        "_score" : 91.52402,
        "_score" : 90.47272,
        "_score" : 75.96907,
        "_score" : 71.13267,
    "max_score" : 140.12831,
        "_score" : 140.12831,
        "_score" : 100.34806,
        "_score" : 77.94006,
    "max_score" : 98.82846,
        "_score" : 98.82846,
        "_score" : 81.944954,
        "_score" : 79.832344,
        "_score" : 79.41739,
        "_score" : 78.47116,
        "_score" : 78.304855,
        "_score" : 78.25241,
        "_score" : 78.155525,
        "_score" : 77.41552,
        "_score" : 77.25188,'

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
     echo "Usage: $0 project2.zip" 
     exit
fi

if [ `hostname` != "cs246" ]; then
     error_exit "You need to run this script within the class virtual machine" 
fi

curl -s -XPOST 'localhost:9200/?pretty' > /dev/null
if [ $? -ne 0 ]; then
     error_exit "Cannot connect to the Elasticsearch server. Have you started it?"
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


echo "Getting data files to test your code..."
retrieve_file "http://oak.cs.ucla.edu/classes/cs246/projects/project2.zip" e6388177a8a88b3729cce76deb86eaa8bc1a90de "/tmp/retrieve-cache/"
mkdir -p data
unzip -q "/tmp/retrieve-cache/project2.zip" data/simplewiki-abstract.json data/benchmark.txt benchmark.sh

echo "Deleting any old indexes..."
curl -s -XDELETE 'localhost:9200/*?pretty' &> /dev/null

echo
echo "Running gradle assemble..."

gradle assemble &> /dev/null
if [ $? -eq 0 ]; then
	echo "SUCCESS!!"
else
	error_exit "Gradle build FAILED."
fi

echo "Installing plugin..."
PLUGINS=`/usr/share/elasticsearch/bin/elasticsearch-plugin list`
for PLUGIN in ${PLUGINS}
do
    echo "password" | sudo -S /usr/share/elasticsearch/bin/elasticsearch-plugin remove ${PLUGIN}
done

chmod +x install-plugin.sh
echo "password" | sudo -S ./install-plugin.sh &> /dev/null

echo "Waiting for elasticsearch to restart..."
for i in `seq 1 180`;
do
    curl -s 'localhost:9200' &> /dev/null
    if [ $? -eq 0 ]; then
        break;
    else
        sleep 1;
    fi
done
curl -s 'localhost:9200' &> /dev/null
if [ $? -ne 0 ]; then
    error_exit "Elasticsearech is not responding for 3 minutes."
fi

echo "Running build.sh..."
chmod +x build.sh
chmod +x benchmark.sh

./build.sh &> /dev/null
curl -s -XPOST 'localhost:9200/_flush?pretty' > /dev/null

# check the document count in each index to ensure that all documents have been added
function check_count() {
    count_result=`curl -s -XGET "localhost:9200/$1/_count?pretty"`
    echo "$count_result" | grep count | grep $2 &> /dev/null
    if [ $? -ne 0 ]
    then
        doc_count=$( echo "$count_result" | grep count )
        echo "[Warning] INDEX $1 is supposed to have $2 documents, but it reports$doc_count"
        COUNT_CHECK_FAILED=1
    fi
}

for i in `seq 1 6`;
do
    COUNT_CHECK_FAILED=0
    check_count "task1a" 128455
    check_count "task1b" 128455
    check_count "task2" 128455
    check_count "task3b" 128455

    if [ $COUNT_CHECK_FAILED -eq 0 ]; then
        break;
    else
        echo "Waiting to catch up with index construction...."
        sleep 30;
    fi
done
if [ $COUNT_CHECK_FAILED -ne 0 ]; then
    error_exit "Document counts in your indices are incorrect."
fi


# run test on $1 index with expected result $2
function test_task() {
    test_result=`./benchmark.sh $1` 
    echo "$test_result" | grep Average | grep $2 &> /dev/null
    if [ $? -eq 0 ]
    then
        echo "SUCCESS!!"
        return 0
    else
        result=$( echo "$test_result" | grep Average )
        echo "ERROR: Results from $1 are incorrect."
        echo "Correct result: $2, Your result: $result"
        echo "Details:"
        echo "$test_result"
        TEST_FAILED=1
        return 1
    fi
}

TEST_FAILED=0
echo
echo "Testing task1a..."
test_task "task1a" 0.2571

echo
echo "Testing task1b..."
test_task "task1b" 0.2594

echo
echo "Testing task2..."
test_task "task2" 0.2588

chmod +x task3a.sh 

echo
echo "Testing task3a..."
for query in "${queries[@]}"
do
    ./task3a.sh $query | grep score &>> task3a.txt
done

diff -w task3a.txt <(echo "$task3a") &> /dev/null
if [ $? -eq 0 ]
then
    echo "SUCCESS!"
else
    echo "Query rankings from task3a incorrect."
    diff -w task3a.txt <(echo "$task3a")
    TEST_FAILED=1
fi

echo 
echo "Testing task3b..."
test_task "task3b" 0.2249

# clean up
rm -rf ${TMP_DIR}
exit $TEST_FAILED