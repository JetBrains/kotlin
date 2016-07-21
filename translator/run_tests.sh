#!/bin/bash

if [ ! -d "src/test/kotlin/tests/linked" ]; then
    # Создать папку, только если ее не было
    mkdir src/test/kotlin/tests/linked
fi

DIRECTORY="src/test/kotlin/tests"
MAIN="$DIRECTORY/linked/main.c"
for i in $( ls "$DIRECTORY/input"); do
	rm -f $DIRECTORY/linked/*
	TEST=`basename $i ".txt"`
	echo test: $TEST
	echo "#include <stdlib.h>" >> $MAIN
        echo "#include <stdio.h>" >> $MAIN
        echo "#include <assert.h>"   >> $MAIN

        echo "int main(){"   >> $MAIN
	cat "$DIRECTORY/input/$i" | while read LINE
	do
		echo " assert($LINE);"   >> $MAIN
		echo " printf(\"OK: $LINE\n\");" >> $MAIN
	done
	
	echo  "printf(\"TEST RESULT: OK\n\");" >> $MAIN


        echo "return 0;}"   >> $MAIN
	
	clang-3.6 -S -emit-llvm $DIRECTORY/linked/main.c -o $DIRECTORY/linked/main.ll -Wno-implicit-function-declaration
	rm -f $DIRECTORY/linked/main.c

	cp ./src/main/resources/kotlib/linked/* $DIRECTORY/linked/

	if [ -f "$DIRECTORY/c/$TEST.c" ]
	then
	    clang-3.6 -S -emit-llvm "$DIRECTORY/c/$TEST.c" -o $DIRECTORY/linked/$TEST"_c.ll" -Wno-implicit-function-declaration
	fi

	java -jar build/libs/ast-kotlin-1.0-SNAPSHOT.jar  $DIRECTORY/kotlin/$TEST.kt > $DIRECTORY/linked/$TEST.ll
	llvm-link-3.6 $DIRECTORY/linked/* > $DIRECTORY/linked/run.ll
	lli-3.6 $DIRECTORY/linked/run.ll
	
done


