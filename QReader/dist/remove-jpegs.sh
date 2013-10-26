#! /bin/bash
# Recursively removes all jpg files from folders
find . -name \00*.jpg -exec rm \-f {} \;
