#!/bin/bash

#
# Measure the execution time on a given corpus file.
#

. ./setup.sh

#
# Which algorithms to test?
#
ALGORITHMS="SKEW DIVSUFSORT BPR DEEP_SHALLOW QSUFSORT"
#ALGORITHMS="DIVSUFSORT QSUFSORT"

#
# Check parameters.
#

CORPUS_DIR=`dirname $0`/corpus

for file in `find ${CORPUS_DIR} -type f -print`; do
	
	#
	# Run evaluations.
	#
	OUTPUT_DIR=results/$file
	mkdir -p $OUTPUT_DIR

	for algorithm in $ALGORITHMS; do
	    if [ -f $OUTPUT_DIR/$algorithm.log ]; then
	        echo "Skipping: $algorithm (log exists)."
	        continue
	    fi

	    run_java org.jsuffixarrays.TimeOnFile \
	        $algorithm \
	        --rounds 10 --warmup-rounds 5 \
	        $file \
	    2>$OUTPUT_DIR/$algorithm.err | tee $OUTPUT_DIR/$algorithm.log

	    # Remove empty logs.
	    if [ ! -s $OUTPUT_DIR/$algorithm.err ]; then
	        rm $OUTPUT_DIR/$algorithm.err
	    fi
	done

	# Collect averages. 
	rm -f $OUTPUT_DIR/averages
	for algorithm in $ALGORITHMS; do
	    ./avg-corpus-file.rb < $OUTPUT_DIR/$algorithm.log >> $OUTPUT_DIR/averages 
	done

	#
	# Render plots.
	#

	#./render-corpus-file.sh $OUTPUT_DIR/averages $OUTPUT_DIR/averages-plot

done 
