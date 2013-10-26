#! /bin/bash

absolute_filename=$(readlink -m $1)
directory=$(dirname $absolute_filename)
pushd $directory
convert $absolute_filename -coalesce %03d.png
popd
