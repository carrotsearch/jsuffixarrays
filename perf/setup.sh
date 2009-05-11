#!/bin/bash

#
# Check preconditions, setup classpath, etc.
#

if [ ! -f ../tmp/perf.classpath ]; then
	(cd ..; ant )
fi

export PERF_CLASSPATH=`cat ../tmp/perf.classpath`

#
# Java interpreters.
#

run_java() {
    # SUN JVM 1.6
    java -cp $PERF_CLASSPATH \
	-Xmx1g -server \
	$@ 
}

