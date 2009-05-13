#!/bin/bash

#
# Measure the execution time on random input, fixed input size and varying alphabet (LCP).
#

. ./setup.sh

#
# Run evaluations.
#
OUTPUT_DIR=results/random-alphabet
mkdir -p $OUTPUT_DIR

for algorithm in $ALGORITHMS; do
    if [ -f $OUTPUT_DIR/$algorithm.log ]; then
        echo "Skipping: $algorithm (log exists)."
        continue
    fi

    run_java org.jsuffixarrays.TimeOnAlphabetSize \
        $algorithm \
        --input-size 5000000 \
        --start-alphabet 2 --increment 2 \
        --rounds 20 --warmup-rounds 2 --samples 1 \
    | tee $OUTPUT_DIR/$algorithm.log
done

#
# Render plots.
#

./render-alphabet-time.sh $OUTPUT_DIR results/random-alphabet-time
./render-alphabet-lcp.sh  $OUTPUT_DIR results/random-alphabet-lcp

