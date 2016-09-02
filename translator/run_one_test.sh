#!/bin/bash
green='\033[0;32m'
red='\033[0;31m'
lightRed='\033[1;31m'
orange='\033[0;33m'
KOTSTD='../kotstd/kt'

cd ../translator

i=$1
TEST=`basename $i ".txt"`
DIRECTORY="src/test/kotlin/tests"
MAIN="$DIRECTORY/$TEST/linked/main.c"
rm -f $DIRECTORY/$TEST/linked/*
mkdir -p $DIRECTORY/$TEST/linked

echo "#include <stdlib.h>" >> $MAIN
echo "#include <stdio.h>" >> $MAIN
echo "#include <assert.h>" >> $MAIN

echo "int main(){" >> $MAIN
echo "printf(\"%stest: $TEST%s\n\", \"\x1B[33m\", \"\x1B[0m\");" >> $MAIN
cat "$i" | while read LINE
do
	echo " assert($LINE);" >> $MAIN
	echo " printf(\"%s[OK]:%s $LINE\n\", \"\x1B[32m\", \"\x1B[0m\");" >> $MAIN
done

echo "printf(\"TEST RESULT: OK\n\");" >> $MAIN
echo "return 0;}" >> $MAIN

if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${red}Error somewhere in main.c generation${nc}"
	exit 1
fi

clang-3.6 -S -emit-llvm $DIRECTORY/$TEST/linked/main.c -o $DIRECTORY/$TEST/linked/main.ll -Wno-implicit-function-declaration
if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${red}Error building main.c${nc}"
	exit 1
fi

cp ../kotstd/build/stdlib_x86.ll $DIRECTORY/$TEST/linked/
if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${red}Error copying ../kotstd/build/stdlib_x86.ll to ${DIRECTORY}/${TEST}/linked/${nc}"
	exit 1
fi

if [ -f "$DIRECTORY/$TEST/$TEST.c" ]
	then
	clang-3.6 -S -emit-llvm "$DIRECTORY/$TEST/$TEST.c" -o $DIRECTORY/$TEST/linked/$TEST"_c.ll" -Wno-implicit-function-declaration
	if [ $? -ne 0 ]; then
		echo -e "${orange}test: ${TEST}${nc}"
		echo -e "${red}Error building: ${DIRECTORY}/${TEST}/linked/${TEST}_c.ll${nc}"
	fi
fi

java -jar build/libs/translator-1.0.jar -I $KOTSTD $DIRECTORY/$TEST/$TEST.kt > $DIRECTORY/$TEST/linked/$TEST.ll

if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${red}Translation error: ${DIRECTORY}/$TEST/${TEST}.kt${nc}"
fi

llvm-link-3.6 -S $DIRECTORY/$TEST/linked/*.ll > $DIRECTORY/$TEST/linked/run.ll
if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${red}Error linking with llvm${nc}"
fi

lli-3.6 $DIRECTORY/$TEST/linked/run.ll
if [ $? -ne 0 ]; then
	echo -e "${orange}test: ${TEST}${nc}"
	echo -e "${lightRed}Error running test${nc}"
fi
