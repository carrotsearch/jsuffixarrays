#!/bin/bash

#
# Generate and copy all files used in thesis text
#

#./render-all.sh

RESULTS_DIR=`dirname $0`/results/
THESIS_DIR=`dirname $0`/../../thesis/

for dir in `find ${RESULTS_DIR}/corpus  -mindepth 1  -maxdepth 1 -type d  -print`; do
	./table.rb $dir > $dir.tex
done;

cp $RESULTS_DIR/corpus/*tex $THESIS_DIR/tables
cp $RESULTS_DIR/corpus/gauntlet.sum.plot.pdf $THESIS_DIR/figures/results/gauntlet.pdf
cp $RESULTS_DIR/*.pdf $THESIS_DIR/figures/results
