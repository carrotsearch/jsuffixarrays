#!/bin/bash

. ./setup.sh

if [ $# -lt 1 ]; then
    echo "$0 [logs dir] [chart file]"
    exit 1
fi

INPUT_DIR=$1
OUTPUT_FILE=$2-$3
SIZE=$3
#
# Generate gnuplot script.
#
cat >.tmp.gnuplot <<EOF
    set terminal postscript enhanced color dashed

    set datafile missing "-"
    set grid

    set key top left Left reverse

    set xtics border nomirror
    set ytics border nomirror
    set tics scale 1.0

    set title "Average longest common prefix on random input, aphabet size = ${SIZE}"
    set xlabel "input size [millions elements]"
    set ylabel "average LCP"

    set output "${OUTPUT_FILE}.eps"

    set boxwidth -2 absolute

    set style line 1 lt 1 lc rgb "#f0a0a0"
    set style line 1 lt 1 lc rgb "#f0a0a0"

    plot \\
EOF

export IFS=$'\n'
for file in `find ${INPUT_DIR} -name "*${SIZE}.avg.log" -print | sort`; do
cat >>.tmp.gnuplot <<EOF
    "$file" \\
	   using (\$2 / 1000000):(\$1 >= 0 ? \$7 : 1/0) t ""       with lines ls 1, \\
	"" using (\$2 / 1000000):(\$1 >= 0 ? \$7 : 1/0) t ""       with points lc rgb "#000000",     \\
EOF
break
done
echo -e '"" using 1:(1/0) t ""\n\n' >> .tmp.gnuplot

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

