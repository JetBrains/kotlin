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

	if [ -f "$DIRECTORY/c/$TEST.c" ]
	then
	    clang-3.6 -S -emit-llvm "$DIRECTORY/c/$TEST.c" -o $DIRECTORY/linked/$TEST"_c.ll" -Wno-implicit-function-declaration
	fi

	/usr/lib/jvm/default-java/bin/java -Didea.launcher.port=7533 -Didea.launcher.bin.path=/opt/idea-IU-145.1617.8/bin -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/default-java/jre/lib/charsets.jar:/usr/lib/jvm/default-java/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/default-java/jre/lib/ext/dnsns.jar:/usr/lib/jvm/default-java/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/default-java/jre/lib/ext/jaccess.jar:/usr/lib/jvm/default-java/jre/lib/ext/localedata.jar:/usr/lib/jvm/default-java/jre/lib/ext/nashorn.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunec.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/default-java/jre/lib/ext/zipfs.jar:/usr/lib/jvm/default-java/jre/lib/jce.jar:/usr/lib/jvm/default-java/jre/lib/jsse.jar:/usr/lib/jvm/default-java/jre/lib/management-agent.jar:/usr/lib/jvm/default-java/jre/lib/resources.jar:/usr/lib/jvm/default-java/jre/lib/rt.jar:/home/user/Kotlin/carkot/translator/build/classes/main:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.0.1/68cddd9aec83d23f789f72bfdc933db245c4a635/kotlin-stdlib-1.0.1.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler/1.0.3/5dbeb14062996a5c2208861d6364cc97a21b06b8/kotlin-compiler-1.0.3.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-runtime/1.0.1/124852ea8cdd3d89827923b5e79627e8a7c314b2/kotlin-runtime-1.0.1.jar:/opt/idea-IU-145.1617.8/lib/idea_rt.jar com.intellij.rt.execution.application.AppMain MainKt $DIRECTORY/kotlin/$TEST.kt > $DIRECTORY/linked/$TEST.ll
	llvm-link-3.6 $DIRECTORY/linked/* > $DIRECTORY/linked/run.ll
	lli-3.6 $DIRECTORY/linked/run.ll
	
done


