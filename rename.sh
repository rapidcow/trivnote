#!/bin/sh
# usage: cat rebase.txt | sh rename.sh

grep ^REPLACE |
while read -r REPLACE a WITH b
do
	if [ -f "$a" ]
	then
mv "$a" "$b"
	fi
done


