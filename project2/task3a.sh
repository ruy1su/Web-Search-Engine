#!/bin/bash

QUERY=$1

curl -s -XGET 'localhost:9200/task1a/_search?pretty' -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { 
            "match": { 
                "title": {
                    "query": "'$QUERY'",
                    "boost": 10
                }
            }
        },
        {
            "match": { 
                "abstract": "'$QUERY'"
            }
        }
      ],
      "must_not": [
        { 
          "match": {
            "sections": "'$QUERY'" 
          } 
        }
      ]
    }
  }
}
'
