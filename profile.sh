#!/bin/bash
#
# Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

export JDK_11_0=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home
export JAVA_HOME=$JDK_11_0
#PATH=$JAVA_HOME/bin:$PATH

#./gradlew :compiler:fir:modularized-tests:test --tests "org.jetbrains.kotlin.fir.FullPipelineModularizedTest" \
#-Pfir.bench.prefix=/home/user/Workspace/testData/kotlin-mt \
#-Pfir.bench.jps.dir=/home/user/Workspace/testData/kotlin-mt/test-project-model-dump \
#-Pfir.bench.passes=1 \
#-Pfir.bench.use.build.file=true
java -version
java -cp `cat tmp/testClasspath.txt` \
-XX:-TieredCompilation \
-XX:ReservedCodeCacheSize=512m \
-XX:+UseParallelGC \
-Xms8192m \
-Xmx8192m \
-ea \
-Djna.nosys=true \
-Dfir.bench.prefix=/Users/Arseniy.Terekhov/JB/kotlin-new/compiler/fir/modularized-tests/testData \
-Dfir.bench.jps.dir=/Users/Arseniy.Terekhov/JB/kotlin-new/compiler/fir/modularized-tests/testData/test-project-model-dump \
-Dfir.bench.passes=1 \
-Dfir.bench.dump=true \
-Dfir.bench.use.build.file=true \
-Didea.ignore.disabled.plugins=true \
-Didea.home.path=/Users/Arseniy.Terekhov/JB/kotlin-new/build/ideaHomeForTests \
org.jetbrains.kotlin.fir.StandaloneModularizedTestRunner
