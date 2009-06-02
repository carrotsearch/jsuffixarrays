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
    ${JAVA_SUN}/bin/java -Xmx2g -server -cp $PERF_CLASSPATH $@ 
}

# IBM JVM, server mode.
ibm_java() {
    check_java $JAVA_IBM, "JAVA_IBM"
    ${JAVA_IBM}/bin/java -Xmx2g -server -cp $PERF_CLASSPATH $@ 
}

# JROCKIT JVM, server mode.
jrockit_java() {
    check_java $JAVA_JROCKIT, "JAVA_JROCKIT"
    ${JAVA_JROCKIT}/bin/java -Xmx2g -server -cp $PERF_CLASSPATH $@ 
}


# HARMONY JVM, server mode.
harmony_java() {
    check_java $JAVA_HARMONY, "JAVA_HARMONY"
    ${JAVA_HARMONY}/bin/java -Xmx2g -server -cp $PERF_CLASSPATH $@ 
}




# Default Java for tests that don't make this distinction.
run_java() {
    sun_java  $@ 
}

#
# Install ctrl-c traps.
# 

trap 'interrupt' 2

interrupt() {
    echo "Interrupted."
    exit 1
}

