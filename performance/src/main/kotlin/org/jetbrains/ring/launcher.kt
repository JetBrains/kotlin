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

import kotlin.system.measureNanoTime

val BENCHMARK_SIZE = 100

//-----------------------------------------------------------------------------//

class Launcher(val numWarmIterations: Int, val numMeasureIterations: Int) {
    val results = mutableListOf<Long>()

    fun launch(benchmark: () -> Any?) {
        var i = numWarmIterations
        var j = numMeasureIterations

        while (i-- > 0) benchmark()
        val time = measureNanoTime {
            while (j-- > 0) benchmark()
        }

        val timeNormalized = time / numMeasureIterations
        results.add(timeNormalized)
        printResult(benchmark.toString(), timeNormalized)
    }

    //-------------------------------------------------------------------------//

    fun printResult(name: String, time: Long) {
        val niceName = name.replace(" (Kotlin reflection is not available)", "").padEnd(45, ' ')
        val niceTime = time.toString().padStart(10, ' ')
        println("  $niceName : $niceTime")
    }

    //-------------------------------------------------------------------------//

    fun runBenchmarks(): Long {
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

        return average()
    }


    //-------------------------------------------------------------------------//

    fun average(): Long {
        val total = results.fold(0L) { total, next -> total + next }
        return total / results.size
    }

    //-------------------------------------------------------------------------//

    fun runAbstractMethodBenchmark() {
        println("\nAbstractMethodBenchmark")
        val benchmark = AbstractMethodBenchmark()

        launch(benchmark::sortStrings)
        launch(benchmark::sortStringsWithComparator)
    }

    //-------------------------------------------------------------------------//

    fun runClassArrayBenchmark() {
        println("\nClassArrayBenchmark")
        val benchmark = ClassArrayBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFiltered)
        launch(benchmark::countFilteredLocal)
    }

    //-------------------------------------------------------------------------//

    fun runClassBaselineBenchmark() {
        println("\nClassBaselineBenchmark")
        val benchmark = ClassBaselineBenchmark()

        launch(benchmark::consume)
        launch(benchmark::consumeField)
        launch(benchmark::allocateList)
        launch(benchmark::allocateArray)
        launch(benchmark::allocateListAndFill)
        launch(benchmark::allocateListAndWrite)
        launch(benchmark::allocateArrayAndFill)
    }

    //-------------------------------------------------------------------------//

    fun runClassListBenchmark() {
        println("\nClassListBenchmark")
        val benchmark = ClassListBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterAndCountWithLambda)
        launch(benchmark::filterWithLambda)
        launch(benchmark::mapWithLambda)
        launch(benchmark::countWithLambda)
        launch(benchmark::filterAndMapWithLambda)
        launch(benchmark::filterAndMapWithLambdaAsSequence)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFiltered)
        launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runClassStreamBenchmark() {
        println("\nClassStreamBenchmark")
        val benchmark = ClassStreamBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFiltered)
        launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runCompanionObjectBenchmark() {
        println("\nCompanionObjectBenchmark")
        val benchmark = CompanionObjectBenchmark()

        launch(benchmark::invokeRegularFunction)
        launch(benchmark::invokeJvmStaticFunction)
    }

    //-------------------------------------------------------------------------//

    fun runDefaultArgumentBenchmark() {
        println("\nDefaultArgumentBenchmark")
        val benchmark = DefaultArgumentBenchmark()
        benchmark.setup()

        launch(benchmark::testOneOfTwo)
        launch(benchmark::testTwoOfTwo)
        launch(benchmark::testOneOfFour)
        launch(benchmark::testFourOfFour)
        launch(benchmark::testOneOfEight)
        launch(benchmark::testEightOfEight)
    }

    //-------------------------------------------------------------------------//

    fun runElvisBenchmark() {
        println("\nElvisBenchmark")
        val benchmark = ElvisBenchmark()
        benchmark.setup()

        launch(benchmark::testElvis)
    }

    //-------------------------------------------------------------------------//

    fun runEulerBenchmark() {
        println("\nEulerBenchmark")
        val benchmark = EulerBenchmark()

        launch(benchmark::problem1bySequence)
        launch(benchmark::problem1)
        launch(benchmark::problem2)
        launch(benchmark::problem4)
        launch(benchmark::problem8)
        launch(benchmark::problem9)
        launch(benchmark::problem14)
        launch(benchmark::problem14full)
    }


    //-------------------------------------------------------------------------//

    fun runFibonacciBenchmark() {
        println("\nFibonacciBenchmark")
        val benchmark = FibonacciBenchmark()
        launch(benchmark::calcClassic)
        launch(benchmark::calc)
        launch(benchmark::calcWithProgression)
        launch(benchmark::calcSquare)
    }

    //-------------------------------------------------------------------------//

    fun runInlineBenchmark() {
        println("\nInlineBenchmark")
        val benchmark = InlineBenchmark()
        launch(benchmark::calculate)
        launch(benchmark::calculateInline)
        launch(benchmark::calculateGeneric)
        launch(benchmark::calculateGenericInline)
    }

    //-------------------------------------------------------------------------//

    fun runIntArrayBenchmark() {
        println("\nIntArrayBenchmark")
        val benchmark = IntArrayBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterSomeAndCount)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterSome)
        launch(benchmark::filterPrime)
        launch(benchmark::filterManual)
        launch(benchmark::filterSomeManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFilteredSomeManual)
        launch(benchmark::countFilteredPrimeManual)
        launch(benchmark::countFiltered)
        launch(benchmark::countFilteredSome)
        launch(benchmark::countFilteredPrime)
        launch(benchmark::countFilteredLocal)
        launch(benchmark::countFilteredSomeLocal)
        launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runIntBaselineBenchmark() {
        println("\nIntBaselineBenchmark")
        val benchmark = IntBaselineBenchmark()

        launch(benchmark::consume)
        launch(benchmark::allocateList)
        launch(benchmark::allocateArray)
        launch(benchmark::allocateListAndFill)
        launch(benchmark::allocateArrayAndFill)
    }

    //-------------------------------------------------------------------------//

    fun runIntListBenchmark() {
        println("\nIntListBenchmark")
        val benchmark = IntListBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFiltered)
        launch(benchmark::countFilteredLocal)
        launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runIntStreamBenchmark() {
        println("\nIntStreamBenchmark")
        val benchmark = IntStreamBenchmark()
        benchmark.setup()

        launch(benchmark::copy)
        launch(benchmark::copyManual)
        launch(benchmark::filterAndCount)
        launch(benchmark::filterAndMap)
        launch(benchmark::filterAndMapManual)
        launch(benchmark::filter)
        launch(benchmark::filterManual)
        launch(benchmark::countFilteredManual)
        launch(benchmark::countFiltered)
        launch(benchmark::countFilteredLocal)
        launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runLambdaBenchmark() {
        println("\nLambdaBenchmark")
        val benchmark = LambdaBenchmark()
        benchmark.setup()

        launch(benchmark::noncapturingLambda)
        launch(benchmark::noncapturingLambdaNoInline)
        launch(benchmark::capturingLambda)
        launch(benchmark::capturingLambdaNoInline)
        launch(benchmark::mutatingLambda)
        launch(benchmark::mutatingLambdaNoInline)
        launch(benchmark::methodReference)
        launch(benchmark::methodReferenceNoInline)
    }

    //-------------------------------------------------------------------------//

    fun runLoopBenchmark() {
        println("\nLoopBenchmark")
        val benchmark = LoopBenchmark()
        benchmark.setup()

        launch(benchmark::arrayLoop)
        launch(benchmark::arrayIndexLoop)
        launch(benchmark::rangeLoop)
        launch(benchmark::arrayListLoop)
        launch(benchmark::arrayWhileLoop)
        launch(benchmark::arrayForeachLoop)
        launch(benchmark::arrayListForeachLoop)
    }

    //-------------------------------------------------------------------------//

    fun runMatrixMapBenchmark() {
        println("\nMatrixMapBenchmark")
        val benchmark = MatrixMapBenchmark()

        launch(benchmark::add)
    }

    //-------------------------------------------------------------------------//

    fun runParameterNotNullAssertionBenchmark() {
        println("\nParameterNotNullAssertionBenchmark")
        val benchmark = ParameterNotNullAssertionBenchmark()

        launch(benchmark::invokeOneArgWithNullCheck)
        launch(benchmark::invokeOneArgWithoutNullCheck)
        launch(benchmark::invokeTwoArgsWithNullCheck)
        launch(benchmark::invokeTwoArgsWithoutNullCheck)
        launch(benchmark::invokeEightArgsWithNullCheck)
        launch(benchmark::invokeEightArgsWithoutNullCheck)
    }

    //-------------------------------------------------------------------------//

    fun runPrimeListBenchmark() {
        println("\nPrimeListBenchmark")
        val benchmark = PrimeListBenchmark()

        launch(benchmark::calcDirect)
        launch(benchmark::calcEratosthenes)
    }

    //-------------------------------------------------------------------------//

    fun runStringBenchmark() {
        println("\nStringBenchmark")
        val benchmark = StringBenchmark()
        benchmark.setup()

        launch(benchmark::stringConcat)
        launch(benchmark::stringConcatNullable)
        launch(benchmark::stringBuilderConcat)
        launch(benchmark::stringBuilderConcatNullable)
        launch(benchmark::summarizeSplittedCsv)
    }

    //-------------------------------------------------------------------------//

    fun runSwitchBenchmark() {
        println("\nSwitchBenchmark")
        val benchmark = SwitchBenchmark()
        benchmark.setupInts()
        benchmark.setupStrings()
        benchmark.setupEnums()
        benchmark.setupSealedClassses()

        launch(benchmark::testSparseIntSwitch)
        launch(benchmark::testDenseIntSwitch)
        launch(benchmark::testConstSwitch)
        launch(benchmark::testObjConstSwitch)
        launch(benchmark::testVarSwitch)
        launch(benchmark::testStringsSwitch)
        launch(benchmark::testEnumsSwitch)
        launch(benchmark::testDenseEnumsSwitch)
        launch(benchmark::testSealedWhenSwitch)
    }

    //-------------------------------------------------------------------------//

    fun runWithIndiciesBenchmark() {
        println("\nWithIndiciesBenchmark")
        val benchmark = WithIndiciesBenchmark()
        benchmark.setup()

        launch(benchmark::withIndicies)
        launch(benchmark::withIndiciesManual)
    }
}