#!/bin/bash

. ./setup.sh

if [ $# -lt 1 ]; then
    echo "$0 [corpus summary file] [chart file]"
    exit 1
fi

SUMMARY=$1
OUTPUT_FILE=$2

#
# Generate gnuplot script.
#
cat >.tmp.gnuplot <<EOF
    set terminal postscript enhanced color dashed

    set datafile missing "-"
    set grid

    set key top right

    set xtics border nomirror
    set ytics border nomirror
    set tics scale 1.0
    set yrange [0:*]
    unset title
    unset xlabel
    set ylabel "time [s]"

    set output "${OUTPUT_FILE}.eps"

    set style fill solid 0.1
    set style data histograms

    set boxwidth 1 relative
    set style histogram clustered gap 1

    set style line 1  lt rgb "#8080ff" lw 2 pt 7
    set style line 10 lt rgb "#e04040" lw 2 pt 6

    plot "${SUMMARY}" \
	   using 1:xticlabels(2) ls 1  t ""
EOF

#
# Render gnuplot.
#
gnuplot .tmp.gnuplot

#
# Convert to PDF, trimming on the way.
#

cat ${OUTPUT_FILE}.eps | \
ps2eps -q --clip --ignoreBB --gsbbox | \
gs -q -dNOPAUSE -dBATCH -dEPSCrop -dNOCACHE -dPDFSETTINGS=/printer -sPAPERSIZE=a4 \
    -dAutoRotatePages=/PageByPage -sDEVICE=pdfwrite -sOutputFile=${OUTPUT_FILE}.pdf - -c quit

rm .tmp.gnuplot

