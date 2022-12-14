#!/bin/bash
#
# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

export JDK_11_0=/home/user/myjdk/amazon-corretto-11.0.99.8.1-linux-x64
#JAVA_HOME=$JDK_11_0
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
-Dfir.bench.prefix=/home/user/Workspace/testData/kotlin-mt \
-Dfir.bench.jps.dir=/home/user/Workspace/testData/kotlin-mt/test-project-model-dump \
-Dfir.bench.passes=2 \
-Dfir.bench.dump=false \
-Dfir.bench.use.build.file=true \
-Didea.ignore.disabled.plugins=true \
-Didea.home.path=/home/user/Workspace/kotlin/build/ideaHomeForTests \
org.jetbrains.kotlin.fir.StandaloneModularizedTestRunner
