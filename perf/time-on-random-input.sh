#!/bin/bash

#
# Measure the execution time on random input, constant alphabet size,
# increasing input size.
#

. ./setup.sh

#
# Which alphabet sizes to test?
#
SIZES="4 100 255"


#
# Which algorithms to test?
#
ALGORITHMS="NS SKEW DIVSUFSORT BPR DEEP_SHALLOW QSUFSORT"


#
# Run evaluations.
#
OUTPUT_DIR=results/random-input
mkdir -p $OUTPUT_DIR

for size in $SIZES; do
	for algorithm in $ALGORITHMS; do
	    if [ -f $OUTPUT_DIR/$algorithm-$size.log ]; then
	        echo "Skipping: $algorithm on size $size (log exists)."
	        continue
	    fi

	    run_java org.jsuffixarrays.TimeOnRandomInput \
	        $algorithm \
	        --alphabet-size $size \
	        --start-size 1000000 --increment 1000000 \
	        --rounds 25 --warmup-rounds 10 --samples 10 \
	    2>$OUTPUT_DIR/$algorithm-$size.err | tee $OUTPUT_DIR/$algorithm-$size.log
	
	    # Remove empty logs.
	    if [ ! -s $OUTPUT_DIR/$algorithm-$size.err ]; then
            rm $OUTPUT_DIR/$algorithm-$size.err
        fi
	done

	#
	#compute statistics
	#
	for algorithm in $ALGORITHMS; do
		./avg-random.rb < $OUTPUT_DIR/$algorithm-$size.log > $OUTPUT_DIR/$algorithm-$size.avg.log
	done


	#
	# Render plots.
	#
	./render-input-time.sh  $OUTPUT_DIR results/random-input-time $size
	./render-input-memory.sh $OUTPUT_DIR results/random-input-memory $size
done