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
import kotlin.system.measureNanoTime

val BENCHMARK_SIZE = 100

//-----------------------------------------------------------------------------//

class Launcher(val numWarmIterations: Int) {
    class Results(val mean: Double, val variance: Double)

    val results = mutableMapOf<String, Results>()

    fun launch(benchmark: () -> Any?): Results {                          // If benchmark runs too long - use coeff to speed it up.
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

        val attempts = 10
        val samples = DoubleArray(attempts)
        for (k in samples.indices) {
            i = autoEvaluatedNumberOfMeasureIteration
            val time = measureNanoTime {
                while (i-- > 0) {
                    benchmark()
                }
                cleanup()
            }
            samples[k] = time * 1.0 / autoEvaluatedNumberOfMeasureIteration
        }
        val mean = samples.sum() / attempts
        val variance = samples.indices.sumByDouble { (samples[it] - mean) * (samples[it] - mean) } / attempts

        return Results(mean, variance)
    }

    //-------------------------------------------------------------------------//

    fun runBenchmarks() {
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
        runOctoTest()

        printResultsNormalized()
    }

    //-------------------------------------------------------------------------//

    fun printResultsAsTime() {
        results.forEach {
            val niceName = "\"${it.key}\"".padEnd(51)
            val niceTime = "${it.value}".padStart(10)
            println("    $niceName to ${niceTime}L,")
        }
    }

    //-------------------------------------------------------------------------//

    fun printResultsNormalized() {
        var totalMean = 0.0
        var totalVariance = 0.0
        results.asSequence().sortedBy { it.key }.forEach {
            val niceName  = it.key.padEnd(50, ' ')
            println("$niceName : ${it.value.mean.toString(9)} : ${kotlin.math.sqrt(it.value.variance).toString(9)}")

            totalMean += it.value.mean
            totalVariance += it.value.variance
        }
        val averageMean = totalMean / results.size
        val averageStdDev = kotlin.math.sqrt(totalVariance) / results.size
        println("\nRingAverage: ${averageMean.toString(9)} : ${averageStdDev.toString(9)}")
    }

    //-------------------------------------------------------------------------//

    fun runAbstractMethodBenchmark() {
        val benchmark = AbstractMethodBenchmark()

        results["AbstractMethod.sortStrings"]               = launch(benchmark::sortStrings)
        results["AbstractMethod.sortStringsWithComparator"] = launch(benchmark::sortStringsWithComparator)
    }

    //-------------------------------------------------------------------------//

    fun runClassArrayBenchmark() {
        val benchmark = ClassArrayBenchmark()
        benchmark.setup()

        results["ClassArray.copy"]                = launch(benchmark::copy)
        results["ClassArray.copyManual"]          = launch(benchmark::copyManual)
        results["ClassArray.filterAndCount"]      = launch(benchmark::filterAndCount)
        results["ClassArray.filterAndMap"]        = launch(benchmark::filterAndMap)
        results["ClassArray.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
        results["ClassArray.filter"]              = launch(benchmark::filter)
        results["ClassArray.filterManual"]        = launch(benchmark::filterManual)
        results["ClassArray.countFilteredManual"] = launch(benchmark::countFilteredManual)
        results["ClassArray.countFiltered"]       = launch(benchmark::countFiltered)
        results["ClassArray.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
    }

    //-------------------------------------------------------------------------//

    fun runClassBaselineBenchmark() {
        val benchmark = ClassBaselineBenchmark()

        results["ClassBaseline.consume"]              = launch(benchmark::consume)
        results["ClassBaseline.consumeField"]         = launch(benchmark::consumeField)
        results["ClassBaseline.allocateList"]         = launch(benchmark::allocateList)
        results["ClassBaseline.allocateArray"]        = launch(benchmark::allocateArray)
        results["ClassBaseline.allocateListAndFill"]  = launch(benchmark::allocateListAndFill)
        results["ClassBaseline.allocateListAndWrite"] = launch(benchmark::allocateListAndWrite)
        results["ClassBaseline.allocateArrayAndFill"] = launch(benchmark::allocateArrayAndFill)
    }

    //-------------------------------------------------------------------------//

    fun runClassListBenchmark() {
        val benchmark = ClassListBenchmark()
        benchmark.setup()

        results["ClassList.copy"]                             = launch(benchmark::copy)
        results["ClassList.copyManual"]                       = launch(benchmark::copyManual)
        results["ClassList.filterAndCount"]                   = launch(benchmark::filterAndCount)
        results["ClassList.filterAndCountWithLambda"]         = launch(benchmark::filterAndCountWithLambda)
        results["ClassList.filterWithLambda"]                 = launch(benchmark::filterWithLambda)
        results["ClassList.mapWithLambda"]                    = launch(benchmark::mapWithLambda)
        results["ClassList.countWithLambda"]                  = launch(benchmark::countWithLambda)
        results["ClassList.filterAndMapWithLambda"]           = launch(benchmark::filterAndMapWithLambda)
        results["ClassList.filterAndMapWithLambdaAsSequence"] = launch(benchmark::filterAndMapWithLambdaAsSequence)
        results["ClassList.filterAndMap"]                     = launch(benchmark::filterAndMap)
        results["ClassList.filterAndMapManual"]               = launch(benchmark::filterAndMapManual)
        results["ClassList.filter"]                           = launch(benchmark::filter)
        results["ClassList.filterManual"]                     = launch(benchmark::filterManual)
        results["ClassList.countFilteredManual"]              = launch(benchmark::countFilteredManual)
        results["ClassList.countFiltered"]                    = launch(benchmark::countFiltered)
        results["ClassList.reduce"]                           = launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runClassStreamBenchmark() {
        val benchmark = ClassStreamBenchmark()
        benchmark.setup()

        results["ClassStream.copy"]                = launch(benchmark::copy)
        results["ClassStream.copyManual"]          = launch(benchmark::copyManual)
        results["ClassStream.filterAndCount"]      = launch(benchmark::filterAndCount)
        results["ClassStream.filterAndMap"]        = launch(benchmark::filterAndMap)
        results["ClassStream.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
        results["ClassStream.filter"]              = launch(benchmark::filter)
        results["ClassStream.filterManual"]        = launch(benchmark::filterManual)
        results["ClassStream.countFilteredManual"] = launch(benchmark::countFilteredManual)
        results["ClassStream.countFiltered"]       = launch(benchmark::countFiltered)
        results["ClassStream.reduce"]              = launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runCompanionObjectBenchmark() {
        val benchmark = CompanionObjectBenchmark()

        results["CompanionObject.invokeRegularFunction"]   = launch(benchmark::invokeRegularFunction)
        results["CompanionObject.invokeJvmStaticFunction"] = launch(benchmark::invokeJvmStaticFunction)
    }

    //-------------------------------------------------------------------------//

    fun runDefaultArgumentBenchmark() {
        val benchmark = DefaultArgumentBenchmark()
        benchmark.setup()

        results["DefaultArgument.testOneOfTwo"]     = launch(benchmark::testOneOfTwo)
        results["DefaultArgument.testTwoOfTwo"]     = launch(benchmark::testTwoOfTwo)
        results["DefaultArgument.testOneOfFour"]    = launch(benchmark::testOneOfFour)
        results["DefaultArgument.testFourOfFour"]   = launch(benchmark::testFourOfFour)
        results["DefaultArgument.testOneOfEight"]   = launch(benchmark::testOneOfEight)
        results["DefaultArgument.testEightOfEight"] = launch(benchmark::testEightOfEight)
    }

    //-------------------------------------------------------------------------//

    fun runElvisBenchmark() {
        val benchmark = ElvisBenchmark()
        benchmark.setup()

        results["Elvis.testElvis"] = launch(benchmark::testElvis)
    }

    //-------------------------------------------------------------------------//

    fun runEulerBenchmark() {
        val benchmark = EulerBenchmark()

        results["Euler.problem1bySequence"] = launch(benchmark::problem1bySequence)
        results["Euler.problem1"]           = launch(benchmark::problem1)
        results["Euler.problem2"]           = launch(benchmark::problem2)
        results["Euler.problem4"]           = launch(benchmark::problem4)
        results["Euler.problem8"]           = launch(benchmark::problem8)
        results["Euler.problem9"]           = launch(benchmark::problem9)
        results["Euler.problem14"]          = launch(benchmark::problem14)
        results["Euler.problem14full"]      = launch(benchmark::problem14full)
    }


    //-------------------------------------------------------------------------//

    fun runFibonacciBenchmark() {
        val benchmark = FibonacciBenchmark()
        results["Fibonacci.calcClassic"]         = launch(benchmark::calcClassic)
        results["Fibonacci.calc"]                = launch(benchmark::calc)
        results["Fibonacci.calcWithProgression"] = launch(benchmark::calcWithProgression)
        results["Fibonacci.calcSquare"]          = launch(benchmark::calcSquare)
    }

    //-------------------------------------------------------------------------//

    fun runInlineBenchmark() {
        val benchmark = InlineBenchmark()
        results["Inline.calculate"]              = launch(benchmark::calculate)
        results["Inline.calculateInline"]        = launch(benchmark::calculateInline)
        results["Inline.calculateGeneric"]       = launch(benchmark::calculateGeneric)
        results["Inline.calculateGenericInline"] = launch(benchmark::calculateGenericInline)
    }

    //-------------------------------------------------------------------------//

    fun runIntArrayBenchmark() {
        val benchmark = IntArrayBenchmark()
        benchmark.setup()

        results["IntArray.copy"]                     = launch(benchmark::copy)
        results["IntArray.copyManual"]               = launch(benchmark::copyManual)
        results["IntArray.filterAndCount"]           = launch(benchmark::filterAndCount)
        results["IntArray.filterSomeAndCount"]       = launch(benchmark::filterSomeAndCount)
        results["IntArray.filterAndMap"]             = launch(benchmark::filterAndMap)
        results["IntArray.filterAndMapManual"]       = launch(benchmark::filterAndMapManual)
        results["IntArray.filter"]                   = launch(benchmark::filter)
        results["IntArray.filterSome"]               = launch(benchmark::filterSome)
        results["IntArray.filterPrime"]              = launch(benchmark::filterPrime)
        results["IntArray.filterManual"]             = launch(benchmark::filterManual)
        results["IntArray.filterSomeManual"]         = launch(benchmark::filterSomeManual)
        results["IntArray.countFilteredManual"]      = launch(benchmark::countFilteredManual)
        results["IntArray.countFilteredSomeManual"]  = launch(benchmark::countFilteredSomeManual)
        results["IntArray.countFilteredPrimeManual"] = launch(benchmark::countFilteredPrimeManual)
        results["IntArray.countFiltered"]            = launch(benchmark::countFiltered)
        results["IntArray.countFilteredSome"]        = launch(benchmark::countFilteredSome)
        results["IntArray.countFilteredPrime"]       = launch(benchmark::countFilteredPrime)
        results["IntArray.countFilteredLocal"]       = launch(benchmark::countFilteredLocal)
        results["IntArray.countFilteredSomeLocal"]   = launch(benchmark::countFilteredSomeLocal)
        results["IntArray.reduce"]                   = launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runIntBaselineBenchmark() {
        val benchmark = IntBaselineBenchmark()

        results["IntBaseline.consume"]              = launch(benchmark::consume)
        results["IntBaseline.allocateList"]         = launch(benchmark::allocateList)
        results["IntBaseline.allocateArray"]        = launch(benchmark::allocateArray)
        results["IntBaseline.allocateListAndFill"]  = launch(benchmark::allocateListAndFill)
        results["IntBaseline.allocateArrayAndFill"] = launch(benchmark::allocateArrayAndFill)
    }

    //-------------------------------------------------------------------------//

    fun runIntListBenchmark() {
        val benchmark = IntListBenchmark()
        benchmark.setup()

        results["IntList.copy"]                = launch(benchmark::copy)
        results["IntList.copyManual"]          = launch(benchmark::copyManual)
        results["IntList.filterAndCount"]      = launch(benchmark::filterAndCount)
        results["IntList.filterAndMap"]        = launch(benchmark::filterAndMap)
        results["IntList.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
        results["IntList.filter"]              = launch(benchmark::filter)
        results["IntList.filterManual"]        = launch(benchmark::filterManual)
        results["IntList.countFilteredManual"] = launch(benchmark::countFilteredManual)
        results["IntList.countFiltered"]       = launch(benchmark::countFiltered)
        results["IntList.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
        results["IntList.reduce"]              = launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runIntStreamBenchmark() {
        val benchmark = IntStreamBenchmark()
        benchmark.setup()

        results["IntStream.copy"]                = launch(benchmark::copy)
        results["IntStream.copyManual"]          = launch(benchmark::copyManual)
        results["IntStream.filterAndCount"]      = launch(benchmark::filterAndCount)
        results["IntStream.filterAndMap"]        = launch(benchmark::filterAndMap)
        results["IntStream.filterAndMapManual"]  = launch(benchmark::filterAndMapManual)
        results["IntStream.filter"]              = launch(benchmark::filter)
        results["IntStream.filterManual"]        = launch(benchmark::filterManual)
        results["IntStream.countFilteredManual"] = launch(benchmark::countFilteredManual)
        results["IntStream.countFiltered"]       = launch(benchmark::countFiltered)
        results["IntStream.countFilteredLocal"]  = launch(benchmark::countFilteredLocal)
        results["IntStream.reduce"]              = launch(benchmark::reduce)
    }

    //-------------------------------------------------------------------------//

    fun runLambdaBenchmark() {
        val benchmark = LambdaBenchmark()
        benchmark.setup()

        results["Lambda.noncapturingLambda"]         = launch(benchmark::noncapturingLambda)
        results["Lambda.noncapturingLambdaNoInline"] = launch(benchmark::noncapturingLambdaNoInline)
        results["Lambda.capturingLambda"]            = launch(benchmark::capturingLambda)
        results["Lambda.capturingLambdaNoInline"]    = launch(benchmark::capturingLambdaNoInline)
        results["Lambda.mutatingLambda"]             = launch(benchmark::mutatingLambda)
        results["Lambda.mutatingLambdaNoInline"]     = launch(benchmark::mutatingLambdaNoInline)
        results["Lambda.methodReference"]            = launch(benchmark::methodReference)
        results["Lambda.methodReferenceNoInline"]    = launch(benchmark::methodReferenceNoInline)
    }

    //-------------------------------------------------------------------------//

    fun runLoopBenchmark() {
        val benchmark = LoopBenchmark()
        benchmark.setup()

        results["Loop.arrayLoop"]            = launch(benchmark::arrayLoop)
        results["Loop.arrayIndexLoop"]       = launch(benchmark::arrayIndexLoop)
        results["Loop.rangeLoop"]            = launch(benchmark::rangeLoop)
        results["Loop.arrayListLoop"]        = launch(benchmark::arrayListLoop)
        results["Loop.arrayWhileLoop"]       = launch(benchmark::arrayWhileLoop)
        results["Loop.arrayForeachLoop"]     = launch(benchmark::arrayForeachLoop)
        results["Loop.arrayListForeachLoop"] = launch(benchmark::arrayListForeachLoop)
    }

    //-------------------------------------------------------------------------//

    fun runMatrixMapBenchmark() {
        val benchmark = MatrixMapBenchmark()

        results["MatrixMap.add"] = launch(benchmark::add)
    }

    //-------------------------------------------------------------------------//

    fun runParameterNotNullAssertionBenchmark() {
        val benchmark = ParameterNotNullAssertionBenchmark()

        results["ParameterNotNull.invokeOneArgWithNullCheck"]       = launch(benchmark::invokeOneArgWithNullCheck)
        results["ParameterNotNull.invokeOneArgWithoutNullCheck"]    = launch(benchmark::invokeOneArgWithoutNullCheck)
        results["ParameterNotNull.invokeTwoArgsWithNullCheck"]      = launch(benchmark::invokeTwoArgsWithNullCheck)
        results["ParameterNotNull.invokeTwoArgsWithoutNullCheck"]   = launch(benchmark::invokeTwoArgsWithoutNullCheck)
        results["ParameterNotNull.invokeEightArgsWithNullCheck"]    = launch(benchmark::invokeEightArgsWithNullCheck)
        results["ParameterNotNull.invokeEightArgsWithoutNullCheck"] = launch(benchmark::invokeEightArgsWithoutNullCheck)
    }

    //-------------------------------------------------------------------------//

    fun runPrimeListBenchmark() {
        val benchmark = PrimeListBenchmark()

        results["PrimeList.calcDirect"]       = launch(benchmark::calcDirect)
        results["PrimeList.calcEratosthenes"] = launch(benchmark::calcEratosthenes)
    }

    //-------------------------------------------------------------------------//

    fun runStringBenchmark() {
        val benchmark = StringBenchmark()
        benchmark.setup()

        results["String.stringConcat"]                = launch(benchmark::stringConcat)
        results["String.stringConcatNullable"]        = launch(benchmark::stringConcatNullable)
        results["String.stringBuilderConcat"]         = launch(benchmark::stringBuilderConcat)
        results["String.stringBuilderConcatNullable"] = launch(benchmark::stringBuilderConcatNullable)
        results["String.summarizeSplittedCsv"]        = launch(benchmark::summarizeSplittedCsv)
    }

    //-------------------------------------------------------------------------//

    fun runSwitchBenchmark() {
        val benchmark = SwitchBenchmark()
        benchmark.setupInts()
        benchmark.setupStrings()
        benchmark.setupEnums()
        benchmark.setupSealedClassses()

        results["Switch.testSparseIntSwitch"]  = launch(benchmark::testSparseIntSwitch)
        results["Switch.testDenseIntSwitch"]   = launch(benchmark::testDenseIntSwitch)
        results["Switch.testConstSwitch"]      = launch(benchmark::testConstSwitch)
        results["Switch.testObjConstSwitch"]   = launch(benchmark::testObjConstSwitch)
        results["Switch.testVarSwitch"]        = launch(benchmark::testVarSwitch)
        results["Switch.testStringsSwitch"]    = launch(benchmark::testStringsSwitch)
        results["Switch.testEnumsSwitch"]      = launch(benchmark::testEnumsSwitch)
        results["Switch.testDenseEnumsSwitch"] = launch(benchmark::testDenseEnumsSwitch)
        results["Switch.testSealedWhenSwitch"] = launch(benchmark::testSealedWhenSwitch)
    }

    //-------------------------------------------------------------------------//

    fun runWithIndiciesBenchmark() {
        val benchmark = WithIndiciesBenchmark()
        benchmark.setup()

        results["WithIndicies.withIndicies"]       = launch(benchmark::withIndicies)
        results["WithIndicies.withIndiciesManual"] = launch(benchmark::withIndiciesManual)
    }

    //-------------------------------------------------------------------------//

    fun runOctoTest() {
        results["OctoTest"] = launch(::octoTest)
    }

    //-------------------------------------------------------------------------//

    fun Double.toString(n: Int): String {
        val str = this.toString()
        if (str.contains('e', ignoreCase = true)) return str

        val len      = str.length
        val pointIdx = str.indexOf('.')
        val dropCnt  = len - pointIdx - n - 1
        if (dropCnt < 1) return str
        return str.dropLast(dropCnt)
    }
}