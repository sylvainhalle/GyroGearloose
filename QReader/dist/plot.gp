#set terminal pdf
#set title "Comparison between algorithms"
set xlabel "Frame nb"
set ylabel "Missed events"
set datafile separator ","
#set key below
set grid
plot "< sort -g out.csv" using 1:2 with lines
