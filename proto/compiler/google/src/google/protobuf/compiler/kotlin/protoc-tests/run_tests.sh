#!/bin/bash
red='\033[0;31m'
nc='\033[0m'
JAVA_TESTS_DIR='kt_java_tests'
JS_TESTS_DIR='kt_js_tests'

echo -e "${red} Running Java Tests${nc}"
cd ${JAVA_TESTS_DIR}
gradle runTests -q
cd ..

if [ $? -ne 0 ]; then
	echo -e "${red}Kotlin <-> Java testing failed! ${nc}"
	exit 1
fi

echo

echo -e "${red} Running JS Tests${nc}"
cd ${JS_TESTS_DIR}
gradle runTests -q
cd ..
if [ $? -ne 0 ]; then
	echo -e "${red}Kotlin <-> JS testing failed! ${nc}"
	exit 1
fi

echo
