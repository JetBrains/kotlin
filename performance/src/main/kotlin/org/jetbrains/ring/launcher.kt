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

import octoTest
import kotlin.math.sqrt
import org.jetbrains.report.BenchmarkResult

val BENCHMARK_SIZE = 100

//-----------------------------------------------------------------------------//

class Launcher(val numWarmIterations: Int, val numberOfAttempts: Int) {
    class Results(val mean: Double, val variance: Double)

    val benchmarkResults = mutableListOf<BenchmarkResult>()

    fun launch(benchmark: () -> Any?, name: String) {                          // If benchmark runs too long - use coeff to speed it up.
        var i = numWarmIterations

        while (i-- > 0) benchmark()
        cleanup()

        var autoEvaluatedNumberOfMeasureIteration = 1
        while (true) {
            var j = autoEvaluatedNumberOfMeasureIteration
            val time = measureNanoTime {
                while (j-- > 0) {
                    benchmark()
                }
                cleanup()
            }
            if (time >= 100L * 1_000_000) // 100ms
                break
            autoEvaluatedNumberOfMeasureIteration *= 2
        }

        val samples = DoubleArray(numberOfAttempts)
        for (k in samples.indices) {
            i = autoEvaluatedNumberOfMeasureIteration
            val time = measureNanoTime {
                while (i-- > 0) {
                    benchmark()
                }
                cleanup()
            }
            val scaledTime = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
            samples[k] = scaledTime
            // Save benchmark object
            benchmarkResults.add(BenchmarkResult(name, BenchmarkResult.Status.PASSED,
                                scaledTime / 1000, scaledTime / 1000,
                                k + 1, numWarmIterations))
        }
    }

    //-------------------------------------------------------------------------//

    fun runBenchmarks(): List<BenchmarkResult> {
        runAbstractMethodBenchmark()
        runClassArrayBenchmark()
        runClassBaselineBenchmark()
        runClassListBenchmark()
        runClassStreamBenchmark()
        runCompanionObjectBenchmark()
        runDefaultArgumentBenchmark()
        runElvisBenchmark()
        runEulerBenchmark()
        runFibonacciBenchmark()
        runForLoopBenchmark()
        runInlineBenchmark()
        runIntArrayBenchmark()
        runIntBaselineBenchmark()
        runIntListBenchmark()
        runIntStreamBenchmark()
        runLambdaBenchmark()
        runLoopBenchmark()
        runMatrixMapBenchmark()
        runParameterNotNullAssertionBenchmark()
        runPrimeListBenchmark()
        runStringBenchmark()
        runSwitchBenchmark()
        runWithIndiciesBenchmark()
        runOctoTest()

        return benchmarkResults
    }

    //-------------------------------------------------------------------------//

    fun runAbstractMethodBenchmark() {
        val benchmark = AbstractMethodBenchmark()

        launch(benchmark::sortStrings, "AbstractMethod.sortStrings")
        launch(benchmark::sortStringsWithComparator, "AbstractMethod.sortStringsWithComparator")
    }

    //-------------------------------------------------------------------------//

    fun runClassArrayBenchmark() {
        val benchmark = ClassArrayBenchmark()
        benchmark.setup()

        launch(benchmark::copy,"ClassArray.copy")
        launch(benchmark::copyManual, "ClassArray.copyManual")
        launch(benchmark::filterAndCount, "ClassArray.filterAndCount")
        launch(benchmark::filterAndMap, "ClassArray.filterAndMap")
        launch(benchmark::filterAndMapManual, "ClassArray.filterAndMapManual")
        launch(benchmark::filter, "ClassArray.filter")
        launch(benchmark::filterManual, "ClassArray.filterManual")
        launch(benchmark::countFilteredManual, "ClassArray.countFilteredManual")
        launch(benchmark::countFiltered, "ClassArray.countFiltered")
        launch(benchmark::countFilteredLocal, "ClassArray.countFilteredLocal")
    }

    //-------------------------------------------------------------------------//

    fun runClassBaselineBenchmark() {
        val benchmark = ClassBaselineBenchmark()

        launch(benchmark::consume, "ClassBaseline.consume")
        launch(benchmark::consumeField, "ClassBaseline.consumeField")
        launch(benchmark::allocateList, "ClassBaseline.allocateList")
        launch(benchmark::allocateArray, "ClassBaseline.allocateArray")
        launch(benchmark::allocateListAndFill, "ClassBaseline.allocateListAndFill")
        launch(benchmark::allocateListAndWrite, "ClassBaseline.allocateListAndWrite")
        launch(benchmark::allocateArrayAndFill, "ClassBaseline.allocateArrayAndFill")
    }

    //-------------------------------------------------------------------------//

    fun runClassListBenchmark() {
        val benchmark = ClassListBenchmark()
        benchmark.setup()

        launch(benchmark::copy, "ClassList.copy")
        launch(benchmark::copyManual, "ClassList.copyManual")
        launch(benchmark::filterAndCount, "ClassList.filterAndCount")
        launch(benchmark::filterAndCountWithLambda, "ClassList.filterAndCountWithLambda")
        launch(benchmark::filterWithLambda, "ClassList.filterWithLambda")
        launch(benchmark::mapWithLambda, "ClassList.mapWithLambda")
        launch(benchmark::countWithLambda, "ClassList.countWithLambda")
        launch(benchmark::filterAndMapWithLambda, "ClassList.filterAndMapWithLambda")
        launch(benchmark::filterAndMapWithLambdaAsSequence, "ClassList.filterAndMapWithLambdaAsSequence")
        launch(benchmark::filterAndMap, "ClassList.filterAndMap")
        launch(benchmark::filterAndMapManual, "ClassList.filterAndMapManual")
        launch(benchmark::filter, "ClassList.filter")
        launch(benchmark::filterManual, "ClassList.filterManual")
        launch(benchmark::countFilteredManual, "ClassList.countFilteredManual")
        launch(benchmark::countFiltered, "ClassList.countFiltered")
        launch(benchmark::reduce, "ClassList.reduce")
    }

    //-------------------------------------------------------------------------//

    fun runClassStreamBenchmark() {
        val benchmark = ClassStreamBenchmark()
        benchmark.setup()

        launch(benchmark::copy, "ClassStream.copy")
        launch(benchmark::copyManual, "ClassStream.copyManual")
        launch(benchmark::filterAndCount, "ClassStream.filterAndCount")
        launch(benchmark::filterAndMap, "ClassStream.filterAndMap")
        launch(benchmark::filterAndMapManual, "ClassStream.filterAndMapManual")
        launch(benchmark::filter, "ClassStream.filter")
        launch(benchmark::filterManual, "ClassStream.filterManual")
        launch(benchmark::countFilteredManual, "ClassStream.countFilteredManual")
        launch(benchmark::countFiltered, "ClassStream.countFiltered")
        launch(benchmark::reduce, "ClassStream.reduce")
    }

    //-------------------------------------------------------------------------//

    fun runCompanionObjectBenchmark() {
        val benchmark = CompanionObjectBenchmark()

        launch(benchmark::invokeRegularFunction, "CompanionObject.invokeRegularFunction")
        launch(benchmark::invokeJvmStaticFunction, "CompanionObject.invokeJvmStaticFunction")
    }

    //-------------------------------------------------------------------------//

    fun runDefaultArgumentBenchmark() {
        val benchmark = DefaultArgumentBenchmark()
        benchmark.setup()

        launch(benchmark::testOneOfTwo, "DefaultArgument.testOneOfTwo")
        launch(benchmark::testTwoOfTwo, "DefaultArgument.testTwoOfTwo")
        launch(benchmark::testOneOfFour, "DefaultArgument.testOneOfFour")
        launch(benchmark::testFourOfFour, "DefaultArgument.testFourOfFour")
        launch(benchmark::testOneOfEight, "DefaultArgument.testOneOfEight")
        launch(benchmark::testEightOfEight, "DefaultArgument.testEightOfEight")
    }

    //-------------------------------------------------------------------------//

    fun runElvisBenchmark() {
        val benchmark = ElvisBenchmark()
        benchmark.setup()

        launch(benchmark::testElvis, "Elvis.testElvis")
    }

    //-------------------------------------------------------------------------//

    fun runEulerBenchmark() {
        val benchmark = EulerBenchmark()

        launch(benchmark::problem1bySequence, "Euler.problem1bySequence")
        launch(benchmark::problem1, "Euler.problem1")
        launch(benchmark::problem2, "Euler.problem2")
        launch(benchmark::problem4, "Euler.problem4")
        launch(benchmark::problem8, "Euler.problem8")
        launch(benchmark::problem9, "Euler.problem9")
        launch(benchmark::problem14, "Euler.problem14")
        launch(benchmark::problem14full, "Euler.problem14full")
    }


    //-------------------------------------------------------------------------//

    fun runFibonacciBenchmark() {
        val benchmark = FibonacciBenchmark()
        launch(benchmark::calcClassic, "Fibonacci.calcClassic")
        launch(benchmark::calc, "Fibonacci.calc")
        launch(benchmark::calcWithProgression, "Fibonacci.calcWithProgression")
        launch(benchmark::calcSquare, "Fibonacci.calcSquare")
    }

    //-------------------------------------------------------------------------//

    fun runForLoopBenchmark() {
        val benchmark = ForLoopsBenchmark()

        launch(benchmark::arrayLoop, "ForLoops.arrayLoop")
        launch(benchmark::intArrayLoop, "ForLoops.intArrayLoop")
        launch(benchmark::floatArrayLoop, "ForLoops.floatArrayLoop")
        launch(benchmark::charArrayLoop, "ForLoops.charArrayLoop")
        launch(benchmark::stringLoop, "ForLoops.stringLoop")

        launch(benchmark::arrayIndicesLoop, "ForLoops.arrayIndicesLoop")
        launch(benchmark::intArrayIndicesLoop, "ForLoops.intArrayIndicesLoop")
        launch(benchmark::floatArrayIndicesLoop, "ForLoops.floatArrayIndicesLoop")
        launch(benchmark::charArrayIndicesLoop, "ForLoops.charArrayIndicesLoop")
        launch(benchmark::stringIndicesLoop, "ForLoops.stringIndicesLoop")
    }

    //-------------------------------------------------------------------------//

    fun runInlineBenchmark() {
        val benchmark = InlineBenchmark()
        launch(benchmark::calculate, "Inline.calculate")
        launch(benchmark::calculateInline, "Inline.calculateInline")
        launch(benchmark::calculateGeneric, "Inline.calculateGeneric")
        launch(benchmark::calculateGenericInline, "Inline.calculateGenericInline")
    }

    //-------------------------------------------------------------------------//

    fun runIntArrayBenchmark() {
        val benchmark = IntArrayBenchmark()
        benchmark.setup()

        launch(benchmark::copy, "IntArray.copy")
        launch(benchmark::copyManual, "IntArray.copyManual")
        launch(benchmark::filterAndCount, "IntArray.filterAndCount")
        launch(benchmark::filterSomeAndCount, "IntArray.filterSomeAndCount")
        launch(benchmark::filterAndMap, "IntArray.filterAndMap")
        launch(benchmark::filterAndMapManual, "IntArray.filterAndMapManual")
        launch(benchmark::filter, "IntArray.filter")
        launch(benchmark::filterSome, "IntArray.filterSome")
        launch(benchmark::filterPrime, "IntArray.filterPrime")
        launch(benchmark::filterManual, "IntArray.filterManual")
        launch(benchmark::filterSomeManual, "IntArray.filterSomeManual")
        launch(benchmark::countFilteredManual, "IntArray.countFilteredManual")
        launch(benchmark::countFilteredSomeManual, "IntArray.countFilteredSomeManual")
        launch(benchmark::countFilteredPrimeManual, "IntArray.countFilteredPrimeManual")
        launch(benchmark::countFiltered, "IntArray.countFiltered")
        launch(benchmark::countFilteredSome, "IntArray.countFilteredSome")
        launch(benchmark::countFilteredPrime, "IntArray.countFilteredPrime")
        launch(benchmark::countFilteredLocal, "IntArray.countFilteredLocal")
        launch(benchmark::countFilteredSomeLocal, "IntArray.countFilteredSomeLocal")
        launch(benchmark::reduce, "IntArray.reduce")
    }

    //-------------------------------------------------------------------------//

    fun runIntBaselineBenchmark() {
        val benchmark = IntBaselineBenchmark()

        launch(benchmark::consume, "IntBaseline.consume")
        launch(benchmark::allocateList, "IntBaseline.allocateList")
        launch(benchmark::allocateArray, "IntBaseline.allocateArray")
        launch(benchmark::allocateListAndFill, "IntBaseline.allocateListAndFill")
        launch(benchmark::allocateArrayAndFill, "IntBaseline.allocateArrayAndFill")
    }

    //-------------------------------------------------------------------------//

    fun runIntListBenchmark() {
        val benchmark = IntListBenchmark()
        benchmark.setup()

        launch(benchmark::copy, "IntList.copy")
        launch(benchmark::copyManual, "IntList.copyManual")
        launch(benchmark::filterAndCount, "IntList.filterAndCount")
        launch(benchmark::filterAndMap, "IntList.filterAndMap")
        launch(benchmark::filterAndMapManual, "IntList.filterAndMapManual")
        launch(benchmark::filter, "IntList.filter")
        launch(benchmark::filterManual, "IntList.filterManual")
        launch(benchmark::countFilteredManual, "IntList.countFilteredManual")
        launch(benchmark::countFiltered, "IntList.countFiltered")
        launch(benchmark::countFilteredLocal, "IntList.countFilteredLocal")
        launch(benchmark::reduce, "IntList.reduce")
    }

    //-------------------------------------------------------------------------//

    fun runIntStreamBenchmark() {
        val benchmark = IntStreamBenchmark()
        benchmark.setup()

        launch(benchmark::copy, "IntStream.copy")
        launch(benchmark::copyManual, "IntStream.copyManual")
        launch(benchmark::filterAndCount, "IntStream.filterAndCount")
        launch(benchmark::filterAndMap, "IntStream.filterAndMap")
        launch(benchmark::filterAndMapManual, "IntStream.filterAndMapManual")
        launch(benchmark::filter, "IntStream.filter")
        launch(benchmark::filterManual, "IntStream.filterManual")
        launch(benchmark::countFilteredManual, "IntStream.countFilteredManual")
        launch(benchmark::countFiltered, "IntStream.countFiltered")
        launch(benchmark::countFilteredLocal, "IntStream.countFilteredLocal")
        launch(benchmark::reduce, "IntStream.reduce")
    }

    //-------------------------------------------------------------------------//

    fun runLambdaBenchmark() {
        val benchmark = LambdaBenchmark()
        benchmark.setup()

        launch(benchmark::noncapturingLambda, "Lambda.noncapturingLambda")
        launch(benchmark::noncapturingLambdaNoInline, "Lambda.noncapturingLambdaNoInline")
        launch(benchmark::capturingLambda, "Lambda.capturingLambda")
        launch(benchmark::capturingLambdaNoInline, "Lambda.capturingLambdaNoInline")
        launch(benchmark::mutatingLambda, "Lambda.mutatingLambda")
        launch(benchmark::mutatingLambdaNoInline, "Lambda.mutatingLambdaNoInline")
        launch(benchmark::methodReference, "Lambda.methodReference")
        launch(benchmark::methodReferenceNoInline, "Lambda.methodReferenceNoInline")
    }

    //-------------------------------------------------------------------------//

    fun runLoopBenchmark() {
        val benchmark = LoopBenchmark()
        benchmark.setup()

        launch(benchmark::arrayLoop, "Loop.arrayLoop")
        launch(benchmark::arrayIndexLoop, "Loop.arrayIndexLoop")
        launch(benchmark::rangeLoop, "Loop.rangeLoop")
        launch(benchmark::arrayListLoop, "Loop.arrayListLoop")
        launch(benchmark::arrayWhileLoop, "Loop.arrayWhileLoop")
        launch(benchmark::arrayForeachLoop, "Loop.arrayForeachLoop")
        launch(benchmark::arrayListForeachLoop, "Loop.arrayListForeachLoop")
    }

    //-------------------------------------------------------------------------//

    fun runMatrixMapBenchmark() {
        val benchmark = MatrixMapBenchmark()

        launch(benchmark::add, "MatrixMap.add")
    }

    //-------------------------------------------------------------------------//

    fun runParameterNotNullAssertionBenchmark() {
        val benchmark = ParameterNotNullAssertionBenchmark()

        launch(benchmark::invokeOneArgWithNullCheck, "ParameterNotNull.invokeOneArgWithNullCheck")
        launch(benchmark::invokeOneArgWithoutNullCheck, "ParameterNotNull.invokeOneArgWithoutNullCheck")
        launch(benchmark::invokeTwoArgsWithNullCheck, "ParameterNotNull.invokeTwoArgsWithNullCheck")
        launch(benchmark::invokeTwoArgsWithoutNullCheck, "ParameterNotNull.invokeTwoArgsWithoutNullCheck")
        launch(benchmark::invokeEightArgsWithNullCheck, "ParameterNotNull.invokeEightArgsWithNullCheck")
        launch(benchmark::invokeEightArgsWithoutNullCheck, "ParameterNotNull.invokeEightArgsWithoutNullCheck")
    }

    //-------------------------------------------------------------------------//

    fun runPrimeListBenchmark() {
        val benchmark = PrimeListBenchmark()

        launch(benchmark::calcDirect, "PrimeList.calcDirect")
        launch(benchmark::calcEratosthenes, "PrimeList.calcEratosthenes")
    }

    //-------------------------------------------------------------------------//

    fun runStringBenchmark() {
        val benchmark = StringBenchmark()
        benchmark.setup()

        launch(benchmark::stringConcat, "String.stringConcat")
        launch(benchmark::stringConcatNullable, "String.stringConcatNullable")
        launch(benchmark::stringBuilderConcat, "String.stringBuilderConcat")
        launch(benchmark::stringBuilderConcatNullable, "String.stringBuilderConcatNullable")
        launch(benchmark::summarizeSplittedCsv, "String.summarizeSplittedCsv")
    }

    //-------------------------------------------------------------------------//

    fun runSwitchBenchmark() {
        val benchmark = SwitchBenchmark()
        benchmark.setupInts()
        benchmark.setupStrings()
        benchmark.setupEnums()
        benchmark.setupSealedClassses()

        launch(benchmark::testSparseIntSwitch, "Switch.testSparseIntSwitch")
        launch(benchmark::testDenseIntSwitch, "Switch.testDenseIntSwitch")
        launch(benchmark::testConstSwitch, "Switch.testConstSwitch")
        launch(benchmark::testObjConstSwitch, "Switch.testObjConstSwitch")
        launch(benchmark::testVarSwitch, "Switch.testVarSwitch")
        launch(benchmark::testStringsSwitch, "Switch.testStringsSwitch")
        launch(benchmark::testEnumsSwitch, "Switch.testEnumsSwitch")
        launch(benchmark::testDenseEnumsSwitch, "Switch.testDenseEnumsSwitch")
        launch(benchmark::testSealedWhenSwitch, "Switch.testSealedWhenSwitch")
    }

    //-------------------------------------------------------------------------//

    fun runWithIndiciesBenchmark() {
        val benchmark = WithIndiciesBenchmark()
        benchmark.setup()

        launch(benchmark::withIndicies, "WithIndicies.withIndicies")
        launch(benchmark::withIndiciesManual, "WithIndicies.withIndiciesManual")
    }

    //-------------------------------------------------------------------------//

    fun runOctoTest() {
        launch(::octoTest, "OctoTest")
    }
}