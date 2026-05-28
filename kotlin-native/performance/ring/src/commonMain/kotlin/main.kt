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

package mainHideName

import DeltaBlueBenchmark
import GenericArrayViewBenchmark
import RichardsBenchmark
import SplayBenchmark
import SplayBenchmarkUsingWorkers
import SplayBenchmarkWithMarkHelpers
import kotlinx.benchmark.*
import octoTest
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.ring.*

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class AbstractMethod : SkipWhenBaseOnly() {
    val instance = AbstractMethodBenchmark()

    @Benchmark
    fun sortStrings(bh: Blackhole) {
        instance.sortStrings(bh)
    }

    @Benchmark
    fun sortStringsWithComparator(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.sortStrings(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class AllocationBenchmark : SkipWhenBaseOnly() {
    val instance = org.jetbrains.ring.AllocationBenchmark()

    @Benchmark
    fun allocateObjects(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateObjects(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class ArrayCopyBenchmark {
    val instance = org.jetbrains.ring.ArrayCopyBenchmark()

    @Benchmark
    fun copyInSameArray(bh: Blackhole) {
        instance.copyInSameArray(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class BunnymarkBenchmark {
    val instance = org.jetbrains.ring.BunnymarkBenchmark()

    @Benchmark
    fun testBunnymark(bh: Blackhole) {
        instance.testBunnymark(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Calls : SkipWhenBaseOnly() {
    val instance = CallsBenchmark()

    @Benchmark
    fun finalMethod(bh: Blackhole) {
        instance.finalMethodCall(bh)
    }

    @Benchmark
    fun interfaceMethodMonomorphic(bh: Blackhole) {
        instance.interfaceMethodCall_MonomorphicCallsite(bh)
    }

    @Benchmark
    fun openMethodMonomorphic(bh: Blackhole) {
        instance.classOpenMethodCall_MonomorphicCallsite(bh)
    }

    @Benchmark
    fun parameterBoxUnboxFolding(bh: Blackhole) {
        instance.parameterBoxUnboxFolding(bh)
    }

    @Benchmark
    fun returnBoxUnboxFolding(bh: Blackhole) {
        instance.returnBoxUnboxFolding(bh)
    }

    @Benchmark
    fun interfaceMethodBimorphic(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_BimorphicCallsite(bh)
    }

    @Benchmark
    fun interfaceMethodHexamorphic(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_HexamorphicCallsite(bh)
    }

    @Benchmark
    fun interfaceMethodTrimorphic(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_TrimorphicCallsite(bh)
    }

    @Benchmark
    fun openMethodBimorphic(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.classOpenMethodCall_BimorphicCallsite(bh)
    }

    @Benchmark
    fun openMethodTrimorphic(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.classOpenMethodCall_TrimorphicCallsite(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class Casts {
    val instance = CastsBenchmark()

    @Benchmark
    fun classCast(bh: Blackhole) {
        instance.classCast(bh)
    }

    @Benchmark
    fun interfaceCast(bh: Blackhole) {
        instance.interfaceCast(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ChainableBenchmark {
    val instance = org.jetbrains.ring.ChainableBenchmark()

    @Benchmark
    fun testChainable(bh: Blackhole) {
        instance.testChainable(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ClassArray : SkipWhenBaseOnly() {
    val instance = ClassArrayBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        instance.copy(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        instance.filter(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        instance.countFiltered(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredLocal(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ClassBaseline : SkipWhenBaseOnly() {
    val instance = ClassBaselineBenchmark()

    @Benchmark
    fun consume(bh: Blackhole) {
        instance.consume(bh)
    }

    @Benchmark
    fun consumeField(bh: Blackhole) {
        instance.consumeField(bh)
    }

    @Benchmark
    fun allocateListAndFill(bh: Blackhole) {
        instance.allocateListAndFill(bh)
    }

    @Benchmark
    fun allocateArray(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateArray(bh)
    }

    @Benchmark
    fun allocateArrayAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateArrayAndFill(bh)
    }

    @Benchmark
    fun allocateList(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateList(bh)
    }

    @Benchmark
    fun allocateListAndWrite(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateListAndWrite(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ClassList : SkipWhenBaseOnly() {
    val instance = ClassListBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        instance.copy(bh)
    }

    @Benchmark
    fun mapWithLambda(bh: Blackhole) {
        instance.mapWithLambda(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        instance.filter(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        instance.reduce(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun countWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countWithLambda(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndCountWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCountWithLambda(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterAndMapWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapWithLambda(bh)
    }

    @Benchmark
    fun filterAndMapWithLambdaAsSequence(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapWithLambdaAsSequence(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }

    @Benchmark
    fun filterWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterWithLambda(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ClassStream : SkipWhenBaseOnly() {
    val instance = ClassStreamBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        instance.copy(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        instance.filter(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        instance.reduce(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class CompanionObject : SkipWhenBaseOnly() {
    val instance = CompanionObjectBenchmark()

    @Benchmark
    fun invokeRegularFunction(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeRegularFunction(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ComplexArrays : SkipWhenBaseOnly() {
    val instance = ComplexArraysBenchmark()

    @Benchmark
    fun outerProduct(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.outerProduct(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class CoordinatesSolver {
    val instance = CoordinatesSolverBenchmark()

    @Benchmark
    fun solve(bh: Blackhole) {
        instance.solve(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class DefaultArgument : SkipWhenBaseOnly() {
    val instance = DefaultArgumentBenchmark()

    @Benchmark
    fun testEightOfEight(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testEightOfEight(bh)
    }

    @Benchmark
    fun testFourOfFour(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testFourOfFour(bh)
    }

    @Benchmark
    fun testOneOfEight(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testOneOfEight(bh)
    }

    @Benchmark
    fun testOneOfFour(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testOneOfFour(bh)
    }

    @Benchmark
    fun testOneOfTwo(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testOneOfTwo(bh)
    }

    @Benchmark
    fun testTwoOfTwo(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testTwoOfTwo(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class DeltaBlueHideName {
    val instance = DeltaBlueBenchmark()

    @Benchmark
    fun DeltaBlue(bh: Blackhole) {
        instance.deltaBlue(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Elvis : SkipWhenBaseOnly() {
    val instance = ElvisBenchmark()

    @Benchmark
    fun testElvis(bh: Blackhole) {
        instance.testElvis(bh)
    }

    @Benchmark
    fun testCompositeElvis(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testCompositeElvis(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
// NOTE: only problem4 is slow enough
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class Euler : SkipWhenBaseOnly() {
    val instance = EulerBenchmark()

    @Benchmark
    fun problem14(bh: Blackhole) {
        instance.problem14(bh)
    }

    @Benchmark
    fun problem1bySequence(bh: Blackhole) {
        instance.problem1bySequence(bh)
    }

    @Benchmark
    fun problem9(bh: Blackhole) {
        instance.problem9(bh)
    }

    @Benchmark
    fun problem1(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.problem1(bh)
    }

    @Benchmark
    fun problem14full(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.problem14full(bh)
    }

    @Benchmark
    fun problem2(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.problem2(bh)
    }

    @Benchmark
    fun problem4(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.problem4(bh)
    }

    @Benchmark
    fun problem8(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.problem8(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Fibonacci : SkipWhenBaseOnly() {
    val instance = FibonacciBenchmark()

    @Benchmark
    fun calc(bh: Blackhole) {
        instance.calc(bh)
    }

    @Benchmark
    fun calcClassic(bh: Blackhole) {
        instance.calcClassic(bh)
    }

    @Benchmark
    fun calcSquare(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.calcSquare(bh)
    }

    @Benchmark
    fun calcWithProgression(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.calcWithProgression(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ForLoops : SkipWhenBaseOnly() {
    val instance = ForLoopsBenchmark()

    @Benchmark
    fun arrayIndicesLoop(bh: Blackhole) {
        instance.arrayIndicesLoop(bh)
    }

    @Benchmark
    fun arrayLoop(bh: Blackhole) {
        instance.arrayLoop(bh)
    }

    @Benchmark
    fun charArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.charArrayIndicesLoop(bh)
    }

    @Benchmark
    fun charArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.charArrayLoop(bh)
    }

    @Benchmark
    fun floatArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.floatArrayIndicesLoop(bh)
    }

    @Benchmark
    fun floatArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.floatArrayLoop(bh)
    }

    @Benchmark
    fun intArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.intArrayIndicesLoop(bh)
    }

    @Benchmark
    fun intArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.intArrayLoop(bh)
    }

    @Benchmark
    fun stringArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.stringArrayLoop(bh)
    }

    @Benchmark
    fun stringIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.stringIndicesLoop(bh)
    }

    @Benchmark
    fun stringLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.stringLoop(bh)
    }

    @Benchmark
    fun uIntArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uIntArrayIndicesLoop(bh)
    }

    @Benchmark
    fun uIntArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uIntArrayLoop(bh)
    }

    @Benchmark
    fun uLongArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uLongArrayIndicesLoop(bh)
    }

    @Benchmark
    fun uLongArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uLongArrayLoop(bh)
    }

    @Benchmark
    fun uShortArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uShortArrayIndicesLoop(bh)
    }

    @Benchmark
    fun uShortArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.uShortArrayLoop(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class GenericArrayView : SkipWhenBaseOnly() {
    val instance = GenericArrayViewBenchmark()

    @Benchmark
    fun inlined(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.inlined(bh)
    }

    @Benchmark
    fun manual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.manual(bh)
    }

    @Benchmark
    fun origin(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.origin(bh)
    }

    @Benchmark
    fun specialized(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.specialized(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class GraphSolver {
    val instance = GraphSolverBenchmark()

    @Benchmark
    fun solve(bh: Blackhole) {
        instance.solve(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Inheritance {
    val instance = InheritanceBenchmark()

    @Benchmark
    fun baseCalls(bh: Blackhole) {
        instance.baseCalls(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Inline : SkipWhenBaseOnly() {
    val instance = InlineBenchmark()

    @Benchmark
    fun calculateInline(bh: Blackhole) {
        instance.calculateInline(bh)
    }

    @Benchmark
    fun calculate(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.calculate(bh)
    }

    @Benchmark
    fun calculateGeneric(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.calculateGeneric(bh)
    }

    @Benchmark
    fun calculateGenericInline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.calculateGenericInline(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntArray : SkipWhenBaseOnly() {
    val instance = IntArrayBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copy(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredLocal(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun countFilteredPrime(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredPrime(bh)
    }

    @Benchmark
    fun countFilteredPrimeManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredPrimeManual(bh)
    }

    @Benchmark
    fun countFilteredSome(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredSome(bh)
    }

    @Benchmark
    fun countFilteredSomeLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredSomeLocal(bh)
    }

    @Benchmark
    fun countFilteredSomeManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredSomeManual(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filter(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }

    @Benchmark
    fun filterPrime(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterPrime(bh)
    }

    @Benchmark
    fun filterSome(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterSome(bh)
    }

    @Benchmark
    fun filterSomeAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterSomeAndCount(bh)
    }

    @Benchmark
    fun filterSomeManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterSomeManual(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.reduce(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntBaseline : SkipWhenBaseOnly() {
    val instance = IntBaselineBenchmark()

    @Benchmark
    fun allocateArray(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateArray(bh)
    }

    @Benchmark
    fun allocateArrayAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateArrayAndFill(bh)
    }

    @Benchmark
    fun allocateList(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateList(bh)
    }

    @Benchmark
    fun allocateListAndFill(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.allocateListAndFill(bh)
    }

    @Benchmark
    fun consume(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.consume(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntList : SkipWhenBaseOnly() {
    val instance = IntListBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copy(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredLocal(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filter(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.reduce(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class IntStream : SkipWhenBaseOnly() {
    val instance = IntStreamBenchmark()

    @Benchmark
    fun copy(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copy(bh)
    }

    @Benchmark
    fun copyManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.copyManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredLocal(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredLocal(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun filter(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filter(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndMap(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMap(bh)
    }

    @Benchmark
    fun filterAndMapManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndMapManual(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.reduce(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Iterator : SkipWhenBaseOnly() {
    val instance = IteratorBenchmark()

    @Benchmark
    fun abstractIterable(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.abstractIterable(bh)
    }

    @Benchmark
    fun baseline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.baseline(bh)
    }

    @Benchmark
    fun concreteIterable(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.concreteIterable(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Lambda : SkipWhenBaseOnly() {
    val instance = LambdaBenchmark()

    @Benchmark
    fun capturingLambda(bh: Blackhole) {
        instance.capturingLambda(bh)
    }

    @Benchmark
    fun methodReference(bh: Blackhole) {
        instance.methodReference(bh)
    }

    @Benchmark
    fun noncapturingLambda(bh: Blackhole) {
        instance.noncapturingLambda(bh)
    }

    @Benchmark
    fun capturingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.capturingLambdaNoInline(bh)
    }

    @Benchmark
    fun methodReferenceNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.methodReferenceNoInline(bh)
    }

    @Benchmark
    fun mutatingLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.mutatingLambda(bh)
    }

    @Benchmark
    fun mutatingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.mutatingLambdaNoInline(bh)
    }

    @Benchmark
    fun noncapturingLambdaNoInline(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.noncapturingLambdaNoInline(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LifeHideName : SkipWhenBaseOnly() {
    val instance = LifeBenchmark()

    @Benchmark
    fun Life(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.bench(bh)
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LifeWithMarkHelpersHideName : SkipWhenBaseOnly() {
    val instance = LifeWithMarkHelpersBenchmark()

    @Benchmark
    fun LifeWithMarkHelpers(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.bench(bh)
    }

    @TearDown
    fun cleanup() {
        instance.terminate()
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class LinkedListWithAtomicsBenchmarkHideName {
    val instance = org.jetbrains.ring.LinkedListWithAtomicsBenchmark()

    @Benchmark
    fun LinkedListWithAtomicsBenchmark(bh: Blackhole) {
        instance.benchmark(bh)
    }

}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class LocalObjects : SkipWhenBaseOnly() {
    val instance = LocalObjectsBenchmark()

    @Benchmark
    fun localArray(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.localArray(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Loop : SkipWhenBaseOnly() {
    val instance = LoopBenchmark()

    @Benchmark
    fun arrayForeachLoop(bh: Blackhole) {
        instance.arrayForeachLoop(bh)
    }

    @Benchmark
    fun arrayListForeachLoop(bh: Blackhole) {
        instance.arrayListForeachLoop(bh)
    }

    @Benchmark
    fun arrayLoop(bh: Blackhole) {
        instance.arrayLoop(bh)
    }

    @Benchmark
    fun arrayWhileLoop(bh: Blackhole) {
        instance.arrayWhileLoop(bh)
    }

    @Benchmark
    fun rangeLoop(bh: Blackhole) {
        instance.rangeLoop(bh)
    }

    @Benchmark
    fun arrayIndexLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.arrayIndexLoop(bh)
    }

    @Benchmark
    fun arrayListLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.arrayListLoop(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class MatrixMap {
    val instance = MatrixMapBenchmark()

    @Benchmark
    fun add(bh: Blackhole) {
        instance.add(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class OctoTestHideName {
    @Benchmark
    fun OctoTest(bh: Blackhole) {
        octoTest(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ParameterNotNull : SkipWhenBaseOnly() {
    val instance = ParameterNotNullAssertionBenchmark()

    @Benchmark
    fun invokeEightArgsWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeEightArgsWithNullCheck(bh)
    }

    @Benchmark
    fun invokeEightArgsWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeEightArgsWithoutNullCheck(bh)
    }

    @Benchmark
    fun invokeOneArgWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeOneArgWithNullCheck(bh)
    }

    @Benchmark
    fun invokeOneArgWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeOneArgWithoutNullCheck(bh)
    }

    @Benchmark
    fun invokeTwoArgsWithNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeTwoArgsWithNullCheck(bh)
    }

    @Benchmark
    fun invokeTwoArgsWithoutNullCheck(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.invokeTwoArgsWithoutNullCheck(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class PrimeList {
    val instance = PrimeListBenchmark()

    @Benchmark
    fun calcDirect(bh: Blackhole) {
        instance.calcDirect(bh)
    }

    @Benchmark
    fun calcEratosthenes(bh: Blackhole) {
        instance.calcEratosthenes(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class RichardsHideName {
    val instance = RichardsBenchmark()

    @Benchmark
    fun Richards(bh: Blackhole) {
        instance.runRichards(bh)
    }

}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Singleton {
    val instance = SingletonBenchmark()

    @Benchmark
    fun access(bh: Blackhole) {
        instance.access(bh)
    }

}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class SplayHideName {
    val instance = SplayBenchmark()

    @Benchmark
    fun Splay(bh: Blackhole) {
        instance.runSplay(bh)
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDown()
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class SplayWithMarkHelpersHideName {
    val instance = SplayBenchmarkWithMarkHelpers()

    @Benchmark
    fun SplayWithMarkHelpers(bh: Blackhole) {
        instance.runSplayWithMarkHelpers(bh)
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDownMarkHelpers()
    }
}

@State(Scope.Benchmark)
// Big benchmark, needs more iterations
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class SplayWithWorkersHideName {
    val instance = SplayBenchmarkUsingWorkers()

    @Benchmark
    fun SplayWithWorkers(bh: Blackhole) {
        instance.runSplayWorkers(bh)
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDownWorkers()
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class String : SkipWhenBaseOnly() {
    val instance = StringBenchmark()

    @Benchmark
    fun stringBuilderConcat(bh: Blackhole) {
        instance.stringBuilderConcat(bh)
    }

    @Benchmark
    fun stringBuilderConcatNullable(bh: Blackhole) {
        instance.stringBuilderConcatNullable(bh)
    }

    @Benchmark
    fun stringConcat(bh: Blackhole) {
        instance.stringConcat(bh)
    }

    @Benchmark
    fun summarizeSplittedCsv(bh: Blackhole) {
        instance.summarizeSplittedCsv(bh)
    }

    @Benchmark
    fun stringConcatNullable(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.stringConcatNullable(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class SubList : SkipWhenBaseOnly() {
    val instance = SubListBenchmark()

    @Benchmark
    fun concatenate(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.concatenate(bh)
    }

    @Benchmark
    fun concatenateManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.concatenateManual(bh)
    }

    @Benchmark
    fun countFiltered(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFiltered(bh)
    }

    @Benchmark
    fun countFilteredManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countFilteredManual(bh)
    }

    @Benchmark
    fun countWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.countWithLambda(bh)
    }

    @Benchmark
    fun filterAndCount(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCount(bh)
    }

    @Benchmark
    fun filterAndCountWithLambda(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterAndCountWithLambda(bh)
    }

    @Benchmark
    fun filterManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.filterManual(bh)
    }

    @Benchmark
    fun reduce(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.reduce(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class Switch : SkipWhenBaseOnly() {
    val instance = SwitchBenchmark()

    @Benchmark
    fun testEnumsSwitch(bh: Blackhole) {
        instance.testEnumsSwitch(bh)
    }

    @Benchmark
    fun testSealedWhenSwitch(bh: Blackhole) {
        instance.testSealedWhenSwitch(bh)
    }

    @Benchmark
    fun testStringsSwitch(bh: Blackhole) {
        instance.testStringsSwitch(bh)
    }

    @Benchmark
    fun testConstSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testConstSwitch(bh)
    }

    @Benchmark
    fun testDenseEnumsSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testDenseEnumsSwitch(bh)
    }

    @Benchmark
    fun testDenseIntSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testDenseIntSwitch(bh)
    }

    @Benchmark
    fun testObjConstSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testObjConstSwitch(bh)
    }

    @Benchmark
    fun testSparseIntSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testSparseIntSwitch(bh)
    }

    @Benchmark
    fun testVarSwitch(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.testVarSwitch(bh)
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class WeakRefBenchmark : SkipWhenBaseOnly() {
    val instance = org.jetbrains.ring.WeakRefBenchmark()

    @Benchmark
    fun aliveReference(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.aliveReference(bh)
    }

    @Benchmark
    fun deadReference(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.deadReference(bh)
    }

    @Benchmark
    fun dyingReference(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.dyingReference(bh)
    }

    @TearDown
    fun cleanup() {
        instance.clean()
    }
}

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class WithIndicies : SkipWhenBaseOnly() {
    val instance = WithIndiciesBenchmark()

    @Benchmark
    fun withIndicies(bh: Blackhole) {
        instance.withIndicies(bh)
    }

    @Benchmark
    fun withIndiciesManual(bh: Blackhole) {
        skipWhenBaseOnly()
        instance.withIndiciesManual(bh)
    }
}
