#!/bin/bash

#
# Measure the execution time on a given corpus file.
#

. ./setup.sh

#
# Which algorithms to test?
#
ALGORITHMS="SKEW DIVSUFSORT SAIS BPR DEEP_SHALLOW QSUFSORT"

#
# Check parameters.
#

CORPUS_DIR=`dirname $0`/corpus
if [ ! -d $CORPUS_DIR ]; then
    echo "Corpus of test files should be at: ${CORPUS_DIR}"
    exit 1
fi

if [ -z "$1" ]; then
    echo "Usage: $0 corpus/<path>"
    exit 1
fi

if [ ! -f $1 ]; then
    echo "File not found: $1"
    exit 1
fi

if [ ! "${1:0:6}" = "corpus" ]; then
    echo "Filename should start with 'corpus/': $1"
    exit 1
fi

#
# Run evaluations.
#
OUTPUT_DIR=results/$1
mkdir -p $OUTPUT_DIR

for algorithm in $ALGORITHMS; do
    if [ -f $OUTPUT_DIR/$algorithm.log ]; then
        echo "Skipping: $algorithm (log exists)."
        continue
    fi

    run_java org.jsuffixarrays.TimeOnFile \
        $algorithm \
        --rounds 5 --warmup-rounds 2 \
        $1 \
    2>$OUTPUT_DIR/$algorithm.err | tee $OUTPUT_DIR/$algorithm.log
done

#
# Render plots.
#

#./render-input-time.sh $OUTPUT_DIR results/random-input-time
#./render-input-memory.sh $OUTPUT_DIR results/random-input-memory
