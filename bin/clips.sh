#!/bin/bash

####################################################################################################
#
# Create clips, with based on what's needed. Call this command as e.g.: clips.sh <input.txt>
#
# Content of a <input.txt> shall be, e.g:
#
# -source test.mp4 -start 00:00:00 -stop 10 -result result.mp4 -audio music.mp3 -fade 3 -deshark 3
#
####################################################################################################

if [ $# = 1 ]; then 
	SOURCE=$1
else
	echo Usage: $0 init.txt
	exit
fi

java -jar ~/.m2/repository/se/berkar63/media/clips/0.9-SNAPSHOT/clips-0.9-SNAPSHOT.jar $SOURCE

