#!/bin/bash

CURDIR=`pwd`
PROJECT=`basename $CURDIR`
INSTALLED=`/usr/share/elasticsearch/bin/elasticsearch-plugin list | grep $PROJECT`
[[ ! -z $INSTALLED ]] && /usr/share/elasticsearch/bin/elasticsearch-plugin remove $PROJECT

/usr/share/elasticsearch/bin/elasticsearch-plugin install file:///$CURDIR/build/distributions/$PROJECT-5.6.2.zip
/etc/init.d/elasticsearch restart
