#!/bin/bash

# In case you use the provided ParseJSON.java code for preprocessing the wikipedia dataset, 
# uncomment the following two commands to compile and execute your modified code in this script.
#
javac ParseJSON.java
java ParseJSON

# TASK 1A:
# Create index "task1a" with "wikipage" type using BM25Similarity 
echo 'Task 1A: Create index "task1a" with "wikipage" type using BM25Similarity '
curl -XPOST --header 'Content-Type: application/json' 'localhost:9200/task1a/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"

# TASK 1B:
# Create index "task1b" with "wikipage" type using ClassicSimilarity (Lucene's version of TFIDF implementation)
echo 'Task 1B: Create index "task1b" with "wikipage" type using ClassicSimilarity Lucenes version of TFIDF implementation '
curl -XPUT --header 'Content-Type: application/json' 'localhost:9200/task1b?pretty' -d'
{
  "settings": {
    "index": {
      "similarity": {
        "default": {
          "type": "classic"
        }
      }
    }
  }
}'
curl -XPOST --header 'Content-Type: application/json' 'localhost:9200/task1b/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"

# TASK 2:
# Create index "task2" with "wikipage" type using BM25Similarity with the best k1 and b values that you found
echo 'Task 2 Create index "task2" with "wikipage" type using BM25Similarity with the best k1 and b values that you found'
echo 'My k1 is 0.80, My b is 0.50'
curl -XPUT --header 'Content-Type: application/json' 'localhost:9200/task2?pretty' -d'
{
  "settings": {
    "index": {
      "similarity": {
        "default": {
          "type": "BM25",
          "k1": 0.80,
          "b": 0.50
        }
      }
    }
  }
}'
curl -XPOST --header 'Content-Type: application/json' 'localhost:9200/task2/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"
#curl -XPOST 'localhost:9200/task2/_close'


# Task 3:
# Create index "task3" with "wikipage"
echo 'Task 3b Create index task3 with wikipage'
curl -XPUT --header 'Content-Type: application/json' 'localhost:9200/task3b?pretty' -d'
{
  "settings": {
    "index": {
      "similarity": {
        "default": {
          "type": "cs246-similarity"
        }
      }
    }
  }
}'

curl -XPUT --header 'Content-Type: application/json' 'localhost:9200/task3b/_mapping/wikipage?pretty' -d'
{
  "properties": {
    "clicks": {
      "type": "long",
      "index": "not_analyzed",
      "doc_values": true
    },
    "abstract" : {
      "type" : "text"
    },
    "url" : {
      "type" : "text"
    },
    "title" : {
      "type" : "text"
    },
    "sections" : {
      "type" : "text"
    }
  }
}'

curl -XPOST --header 'Content-Type: application/json' 'localhost:9200/task3b/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"
