#!/bin/bash
green='\033[0;32m'
red='\033[0;31m'
nc='\033[0m'

if [ ! -d "src/test/kotlin/tests/linked" ]; then
	# Create folder if not exists
	mkdir src/test/kotlin/tests/linked
fi

cd ../kotstd && make clean && make
cd ../translator

DIRECTORY="src/test/kotlin/tests"
MAIN="$DIRECTORY/linked/main.c"
for i in $( ls "$DIRECTORY/input" $1); do
	rm -f $DIRECTORY/linked/*
	TEST=`basename $i ".txt"`
	echo -e "${red}test: ${TEST}${nc}"
	echo "#include <stdlib.h>" >> $MAIN
	echo "#include <stdio.h>" >> $MAIN
	echo "#include <assert.h>" >> $MAIN

	echo "int main(){" >> $MAIN
	cat "$DIRECTORY/input/$i" | while read LINE
	do
		echo " assert($LINE);" >> $MAIN
		echo " printf(\"OK: $LINE\n\");" >> $MAIN
	done

	echo "printf(\"TEST RESULT: OK\n\");" >> $MAIN
	echo "return 0;}" >> $MAIN

	clang-3.6 -S -emit-llvm $DIRECTORY/linked/main.c -o $DIRECTORY/linked/main.ll -Wno-implicit-function-declaration
	rm -f $DIRECTORY/linked/main.c

	cp ../kotstd/build/stdlib_x86.ll $DIRECTORY/linked/

	if [ -f "$DIRECTORY/c/$TEST.c" ]
	then
		clang-3.6 -S -emit-llvm "$DIRECTORY/c/$TEST.c" -o $DIRECTORY/linked/$TEST"_c.ll" -Wno-implicit-function-declaration
	fi

	java -jar build/libs/translator-1.0.jar -I ../kotstd/include $DIRECTORY/kotlin/$TEST.kt > $DIRECTORY/linked/$TEST.ll
	llvm-link-3.6 -S $DIRECTORY/linked/* > $DIRECTORY/linked/run.ll
	lli-3.6 $DIRECTORY/linked/run.ll
done
