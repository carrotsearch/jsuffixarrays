. ./setup.sh

./render-alphabet-time.sh results/random-alphabet results/random-alphabet-time
./render-alphabet-lcp.sh  results/random-alphabet results/random-alphabet-lcp

./render-input-time.sh  results/random-input results/random-input-time
./render-input-memory.sh results/random-input results/random-input-memory


CORPUS_DIR=`dirname $0`/corpus
for file in `find ${CORPUS_DIR} -type f -print`; do
	
	OUTPUT_DIR=results/$file
	./render-corpus-file.sh $OUTPUT_DIR/averages $OUTPUT_DIR/averages-plot
	
done	