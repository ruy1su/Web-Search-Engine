#!/bin/bash
# Author: Zixia Weng Jan 29.2019

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
curl -XPUT 'localhost:9200/task2b?pretty' -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "wikipage" : {
      "_all" : {
      	"type": "text", 
      	"analyzer": "whitespace"
      },
      "properties" : {
        "abstract" : {
          "type" :    "text",
          "analyzer": "whitespace"
        },
        "url" : {
          "type" :   "text",
          "analyzer": "whitespace"
        },
        "title" : {
          "type" :   "text",
          "analyzer": "whitespace"
        },
        "sections" : {
          "type" :   "text",
          "analyzer": "whitespace"
        }
      }
    }
  }
}
'
curl -H "Content-Type: application/json" -XPOST 'localhost:9200/task2b/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"


echo 'Task 2C: Create and index with a custom analyzer'
# TASK 2C:
# Create and index with a custom analyzer as specified in Task 2C

curl -XPUT 'localhost:9200/task2c?pretty' -H 'Content-Type: application/json' -d'
{
    "settings": {
        "analysis": {
            "analyzer": {
                "my_analyzer": {
                    "type":         "custom",
                    "char_filter":  "html_strip",
                    "tokenizer":    "standard",
                    "filter":       [ "asciifolding", "lowercase", "snowball", "stop"]
            	}
        	}
		}
	},
	"mappings": {
		"wikipage" : {
			"_all" : {
				"type" : "text", 
				"analyzer" : "my_analyzer"
			},
			"properties" : {
				"abstract" : {
					"type" :    "text",
					"analyzer": "my_analyzer"
				},
				"title" : {
					"type" :   "text",
					"analyzer": "my_analyzer"
				},
				"url": {
					"type" :   "text",
					"analyzer": "my_analyzer"
				},
				"sections": {
					"type" :   "text",
					"analyzer": "my_analyzer"
				}
			}
		}
    }
}
'
curl -H "Content-Type: application/json" -XPOST 'localhost:9200/task2c/wikipage/_bulk?pretty&refresh' --data-binary "@data/out.txt"



