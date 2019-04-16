#!/bin/bash
TMP_DIR=/tmp/p3-grading/
REQUIRED_FILES="log.txt SpellChecker.java"


# usage
if [ $# -ne 1 ]
then
     echo "Usage: $0 project3.zip" 1>&2
     exit
fi

if [ `hostname` != "cs246" ]; then
     echo "ERROR: You need to run this script within the class virtual machine" 1>&2
     exit
fi

ZIP_FILE=$1

# clean any existing files
rm -rf ${TMP_DIR}
mkdir ${TMP_DIR}

# unzip the submission zip file 
if [ ! -f ${ZIP_FILE} ]; then
    echo "ERROR: Cannot find $ZIP_FILE" 1>&2
    rm -rf ${TMP_DIR}
    exit 1
fi
unzip -q -d ${TMP_DIR} ${ZIP_FILE}
if [ "$?" -ne "0" ]; then 
    echo "ERROR: Cannot unzip ${ZIP_FILE} to ${TMP_DIR}"
    rm -rf ${TMP_DIR}
    exit 1
fi

# change directory to the grading folder
cd ${TMP_DIR}

# check the existence of the required files
for FILE in ${REQUIRED_FILES}
do
    if [ ! -f ${FILE} ]; then
        echo "ERROR: Cannot find ${FILE} in the root folder of your zip file" 1>&2
        rm -rf ${TMP_DIR}
        exit 1
    fi
done


# compile, exit if error
echo "Compiling SpellChecker.java..."
javac SpellChecker.java
if [ "$?" -ne "0" ] 
then
	echo "ERROR: Compilation of SpellChecker.java failed" 1>&2
	rm -rf ${TMP_DIR}
	exit 1
fi

# test SpellChecker.java, using test input files
echo "Testing SpellChecker..."
INPUT_FILE1="${TMP_DIR}/test-input.txt"
echo "parallel: pararrel" > ${INPUT_FILE1}
echo "they: tehi" >> ${INPUT_FILE1}
INPUT_FILE2="${TMP_DIR}/dict.txt"
echo -e "parallel\t100" > ${INPUT_FILE2}
echo -e "paragraph\t15" >> ${INPUT_FILE2}
echo -e "paradize\t5" >> ${INPUT_FILE2}
echo -e "they\t100" >> ${INPUT_FILE2}
RESULT=`java SpellChecker ${INPUT_FILE2} ${INPUT_FILE1}`
echo "$RESULT"

res=$( echo "$RESULT" | awk '/FINAL RESULT: / { print $3; }' )
if [ "$res" == "2" ]
then
    echo "SUCCESS!"
else
    echo "ERROR: SpellChecker cannot correct pararrel to parallel or tehi to they."
    echo "       They are within Damerau-Levenshtein edit distance 2!"
    rm -rf ${TMP_DIR}
    exit 1
fi

# clean up
rm -rf ${TMP_DIR}
exit 0