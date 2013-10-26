#! /bin/bash
for imagefile in `ls -v $1/*.jpg`
do
  #echo -ne "$imagefile "
  new_filename="$imagefile.gif"
  convert $imagefile -threshold $2 $new_filename
done
echo ""
