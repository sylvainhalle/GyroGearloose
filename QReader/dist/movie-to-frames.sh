#! /bin/bash

absolute_filename=$(readlink -m $1)
directory=$(dirname $absolute_filename)
pushd $directory
mplayer -vo jpeg $absolute_filename
#mplayer -vo png:z=9 $absolute_filename
popd
