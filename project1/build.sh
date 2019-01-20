#!/bin/bash

# Uncomment the following two commands to compile and execute your modified ParseJSON code in this script.
echo 'Task 1: Compiling ParseJSON'
javac ParseJSON.java
java ParseJSON

javac GetWebpage.java
# java GetWebpage

echo 'Task 2A: Index Using Default Setting'
# TASK 2A:
# Create and index the documents using the default standard analyzer
curl -H "Content-Type: application/json" -XPOST 'localhost:9200/task2a/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"


echo 'Task 2B: Create and index with a whitespace analyzer'
# TASK 2B:
# Create and index with a whitespace analyzer




# TASK 2C:
# Create and index with a custom analyzer as specified in Task 2C




