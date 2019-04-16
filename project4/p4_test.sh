#!/bin/bash
REQUIRED_FILES="task1.py task2.sh topic-words.txt doc-topics.txt topic-labels.txt"
REQUIRED_OUTPUT2="topic-words.txt doc-topics.txt"
ZIP_FILE=$1
TMP_DIR=/tmp/p4-grading/

function error_exit()
{
   echo "ERROR: $1" 1>&2
   rm -rf ${TMP_DIR}
   exit 1
}

function check_files()
{
    for FILE in $1; do
        if [ ! -f ${FILE} ]; then
            error_exit "Cannot find ${FILE} in $2"
        fi
    done
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
     error_exit "Usage: $0 project4.zip"
fi

if [ `hostname` != "cs246" ]; then
     error_exit "You need to run this script within the class virtual machine"
fi


# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# unzip the submission zip file 
if [ ! -f ${ZIP_FILE} ]; then
    error_exit "Cannot find ${ZIP_FILE}"
fi
unzip -q -d ${TMP_DIR} ${ZIP_FILE}
if [ $? -ne 0 ]; then 
    error_exit "Cannot unzip ${ZIP_FILE} to ${TMP_DIR}"
fi

# change directory to the grading folder
cd ${TMP_DIR}

# check the existence of the required files
check_files "${REQUIRED_FILES}" "root folder of the zip file"

# run task1.py and check the existence of output file
echo "Testing Task 1..."

# retrieve sample image
INPUT_PNG="lion-small.png"
OUTPUT_PNG="lion-small.3.png"
OUTPUT_NPY="lion-small.3.npy"
retrieve_file "http://oak.cs.ucla.edu/classes/cs246/projects/project4.zip" c9ebb6e0d6e663860912767ed5b2d6fb778e75ef "/tmp/retrieve-cache/"
unzip -q "/tmp/retrieve-cache/project4.zip" ${INPUT_PNG} 

python3 task1.py ${INPUT_PNG} 3

check_files "${OUTPUT_PNG} ${OUTPUT_NPY}" "the output of task1.py"

FINFO=$(file "${OUTPUT_PNG}")
if [ "$FINFO" != "${OUTPUT_PNG}: PNG image data, 1024 x 640, 8-bit grayscale, non-interlaced" ]; then 
    error_exit "The output of task1.py is wrong"
fi
echo "Finished testing Task 1, SUCCESS!"

# run task2.sh and check the format of the output files
echo "Testing Task 2..."
rm -f "${REQUIRED_OUTPUT2}"

sh task2.sh $MALLET_HOME/sample-data/web/en/ 5 >& /dev/null

check_files "${REQUIRED_OUTPUT2}" "the output of task2.sh"

TP=$(cat topic-words.txt | head -1 | wc -w)
DT=$(cat doc-topics.txt | head -1| wc -w)
if [ $TP != "22" ] || [ $DT != "7" ]; then
    error_exit "The output of task2.sh has wrong format"
fi
echo "Finished testing Task 2, SUCCESS!"

# clean up
rm -rf ${TMP_DIR}
exit 0