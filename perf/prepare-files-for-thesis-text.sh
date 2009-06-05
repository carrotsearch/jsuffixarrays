#!/bin/bash

#
# Generate and copy all files used in thesis text
#

JVMS="ibm jrockit harmony sun"

RESULTS_DIR=`dirname $0`/results/
THESIS_DIR=`dirname $0`/../../thesis/

for dir in `find ${RESULTS_DIR}/corpus  -mindepth 3  -maxdepth 3 -type d  -print`; do
	./table.rb $dir > $dir.tex
done;

for jvm in $JVMS; do 
	cp $RESULTS_DIR/corpus/$jvm/corpus/*tex $THESIS_DIR/tables/$jvm
	cp $RESULTS_DIR/corpus/$jvm/corpus/gauntlet.sum.plot.pdf $THESIS_DIR/figures/results/$jvm-gauntlet.pdf
	cp $RESULTS_DIR/corpus/$jvm/corpus/manzini.sum.plot.pdf $THESIS_DIR/figures/results/$jvm-manzini.pdf
	cp $RESULTS_DIR/*.pdf $THESIS_DIR/figures/results
done

