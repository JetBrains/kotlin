#
# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

./gradlew -info :compiler:fir:modularized-tests:test --tests "org.jetbrains.kotlin.fir.FullPipelineModularizedTest" -Pfir.bench.prefix=/Users/lige/Work/kotlin/modularized/kotlin -Pfir.bench.jps.dir=/Users/lige/Work/kotlin/modularized/kotlin/test-project-model-dump -Pfir.bench.passes=1 -Pfir.bench.dump=true \
-Pfir.modularized.jvm.args="-XX:+PreserveFramePointer \
-Dfir.bench.use.async.profiler.lib=/Users/lige/Work/jvm/async-profiler/async-profiler-2.9-macos/build/libasyncProfiler.so \
-Dfir.bench.use.async.profiler.cmd.start=event=cpu,interval=1ms,threads,start \
-Dfir.bench.use.async.profiler.cmd.stop=collapsed,file=\$SNAPSHOT_DIR/async-profiler-snapshot-\$PASS-ts-%t.collapsed,stop \
-Dfir.bench.snapshot.dir=/Users/lige/Work/kotlin/snapshots/"
