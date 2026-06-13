/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.ring

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

val OBJ = Any()

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ParameterNotNull : SkipWhenBaseOnly() {
    
    fun methodWithOneNotnullParameter(p: Any): Any {
        return p
    }

    private fun privateMethodWithOneNotnullParameter(p: Any): Any {
        return p
    }

    fun methodWithTwoNotnullParameters(p: Any, p2: Any): Any {
        return p
    }

    private fun privateMethodWithTwoNotnullParameters(p: Any, p2: Any): Any {
        return p
    }

    fun methodWithEightNotnullParameters(p: Any, p2: Any, p3: Any, p4: Any, p5: Any, p6: Any, p7: Any, p8: Any): Any {
        return p
    }

    private fun privateMethodWithEightNotnullParameters(p: Any, p2: Any, p3: Any, p4: Any, p5: Any, p6: Any, p7: Any, p8: Any): Any {
        return p
    }

    @Benchmark
    fun invokeOneArgWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(methodWithOneNotnullParameter(OBJ))
    }

    @Benchmark
    fun invokeOneArgWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(privateMethodWithOneNotnullParameter(OBJ))
    }

    @Benchmark
    fun invokeTwoArgsWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(methodWithTwoNotnullParameters(OBJ, OBJ))
    }

    @Benchmark
    fun invokeTwoArgsWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(privateMethodWithTwoNotnullParameters(OBJ, OBJ))
    }

    @Benchmark
    fun invokeEightArgsWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(methodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ))
    }

    @Benchmark
    fun invokeEightArgsWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        bh.consume(privateMethodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ))
    }
}

