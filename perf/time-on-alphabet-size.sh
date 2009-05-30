#!/bin/bash

#
# Measure the execution time on random input, fixed input size and varying alphabet (LCP).
#

. ./setup.sh

#
# Which algorithms to test?
#
ALGORITHMS="NS SKEW DIVSUFSORT BPR DEEP_SHALLOW QSUFSORT"
#ALGORITHMS="DIVSUFSORT QSUFSORT"

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
        --start-alphabet 5 --increment 10 \
        --rounds 25 --warmup-rounds 10 --samples 10 \
    | tee $OUTPUT_DIR/$algorithm.log
done

#
# Compute statistics
#
for algorithm in $ALGORITHMS; do
	./avg-random.rb < $OUTPUT_DIR/$algorithm.log  >  $OUTPUT_DIR/$algorithm.avg.log
done

#
# Render plots.
#

#./render-alphabet-time.sh $OUTPUT_DIR results/random-alphabet-time
#./render-alphabet-lcp.sh  $OUTPUT_DIR results/random-alphabet-lcp

