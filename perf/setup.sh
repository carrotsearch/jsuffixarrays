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

if [ ! -f "local-settings.sh" ]; then
    echo "Copy local-settings.sh.template to local-settings.sh and fill in the defaults."
    exit 1
fi

. local-settings.sh

check_java() {
    if [ -z "$1" ]; then
        echo "Java interpreter not found (property $2)."
        exit 1
    fi
}

# SUN JVM, server mode.
sun_java() {
    check_java $JAVA_SUN, "JAVA_SUN"
    ${JAVA_SUN}/bin/java -Xmx1g -server $@ 
}

# Default Java for tests that don't make this distinction.
run_java() {
    sun_java -cp $PERF_CLASSPATH $@ 
}

#
# Install ctrl-c traps.
# 

trap 'interrupt' 2

interrupt() {
    echo "Interrupted."
    exit 1
}

