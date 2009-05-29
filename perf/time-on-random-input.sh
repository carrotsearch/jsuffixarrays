#!/bin/bash

#
# Measure the execution time on random input, constant alphabet size,
# increasing input size.
#

. ./setup.sh

#
# Which algorithms to test?
#
#ALGORITHMS="NS SKEW DIVSUFSORT BPR DEEP_SHALLOW QSUFSORT"
ALGORITHMS="DIVSUFSORT QSUFSORT"

#
# Run evaluations.
#
OUTPUT_DIR=results/random-input
mkdir -p $OUTPUT_DIR

for algorithm in $ALGORITHMS; do
    if [ -f $OUTPUT_DIR/$algorithm.log ]; then
        echo "Skipping: $algorithm (log exists)."
        continue
    fi

    run_java org.jsuffixarrays.TimeOnRandomInput \
        $algorithm \
        --alphabet-size 32 \
        --start-size 1000000 --increment 1000000 \
        --rounds 2 --warmup-rounds 0 --samples 3 \
    | tee $OUTPUT_DIR/$algorithm.log
done

#
#compute statistics
#
for algorithm in $ALGORITHMS; do
	./avg-random.rb < $OUTPUT_DIR/$algorithm.log > $OUTPUT_DIR/$algorithm.avg.log
done


#
# Render plots.
#

#./render-input-time.sh  $OUTPUT_DIR results/random-input-time
#./render-input-memory.sh $OUTPUT_DIR results/random-input-memory
