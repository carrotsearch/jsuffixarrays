#!/bin/bash

mkdir results
. time-on-alphabet-size.sh  > results/running.logs 2>&1
. time-on-random-input.sh  >> results/running.logs 2>&1
. time-on-corpus-file.sh   >> results/running.logs 2>&1
