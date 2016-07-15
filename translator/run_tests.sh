if [ ! -d "src/test/kotlin/tests/linked" ]; then
    # Создать папку, только если ее не было
    mkdir src/test/kotlin/tests/linked
fi

rm -f src/test/kotlin/tests/linked/*

/usr/lib/jvm/default-java/bin/java -Didea.launcher.port=7540 -Didea.launcher.bin.path=/opt/idea-IU-145.1617.8/bin -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/default-java/jre/lib/charsets.jar:/usr/lib/jvm/default-java/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/default-java/jre/lib/ext/dnsns.jar:/usr/lib/jvm/default-java/jre/lib/ext/icedtea-sound.jar:/usr/lib/jvm/default-java/jre/lib/ext/jaccess.jar:/usr/lib/jvm/default-java/jre/lib/ext/localedata.jar:/usr/lib/jvm/default-java/jre/lib/ext/nashorn.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunec.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/default-java/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/default-java/jre/lib/ext/zipfs.jar:/usr/lib/jvm/default-java/jre/lib/jce.jar:/usr/lib/jvm/default-java/jre/lib/jsse.jar:/usr/lib/jvm/default-java/jre/lib/management-agent.jar:/usr/lib/jvm/default-java/jre/lib/resources.jar:/usr/lib/jvm/default-java/jre/lib/rt.jar:/home/user/Kotlin/carkot/translator/build/classes/test:/home/user/Kotlin/carkot/translator/build/classes/main:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.0.1/68cddd9aec83d23f789f72bfdc933db245c4a635/kotlin-stdlib-1.0.1.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler/1.0.3/5dbeb14062996a5c2208861d6364cc97a21b06b8/kotlin-compiler-1.0.3.jar:/home/user/.gradle/caches/modules-2/files-2.1/junit/junit/4.11/4e031bb61df09069aeb2bffb4019e7a5034a4ee0/junit-4.11.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-runtime/1.0.1/124852ea8cdd3d89827923b5e79627e8a7c314b2/kotlin-runtime-1.0.1.jar:/home/user/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/42a25dc3219429f0e5d060061f71acb49bf010a0/hamcrest-core-1.3.jar:/opt/idea-IU-145.1617.8/lib/idea_rt.jar com.intellij.rt.execution.application.AppMain TestmainKt



cd src/test/kotlin/tests/linked
rm -f main.c
llvm-link * > run.ll
lli run.ll
