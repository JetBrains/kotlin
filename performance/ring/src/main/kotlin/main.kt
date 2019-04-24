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


import org.jetbrains.ring.*
import octoTest
import org.jetbrains.benchmarksLauncher.*
import org.jetbrains.kliopt.*

class RingLauncher(numWarmIterations: Int, numberOfAttempts: Int, prefix: String) : Launcher(numWarmIterations, numberOfAttempts, prefix) {
    val abstractMethodBenchmark = AbstractMethodBenchmark()
    val classArrayBenchmark = ClassArrayBenchmark()
    val classBaselineBenchmark = ClassBaselineBenchmark()
    val classListBenchmark = ClassListBenchmark()
    val classStreamBenchmark = ClassStreamBenchmark()
    val companionObjectBenchmark = CompanionObjectBenchmark()
    val defaultArgumentBenchmark = DefaultArgumentBenchmark()
    val elvisBenchmark = ElvisBenchmark()
    val eulerBenchmark = EulerBenchmark()
    val fibonacciBenchmark = FibonacciBenchmark()
    val forLoopsBenchmark = ForLoopsBenchmark()
    val inlineBenchmark = InlineBenchmark()
    val intArrayBenchmark = IntArrayBenchmark()
    val intBaselineBenchmark = IntBaselineBenchmark()
    val intListBenchmark = IntListBenchmark()
    val intStreamBenchmark = IntStreamBenchmark()
    val lambdaBenchmark = LambdaBenchmark()
    val loopBenchmark = LoopBenchmark()
    val matrixMapBenchmark = MatrixMapBenchmark()
    val parameterNotNullAssertionBenchmark = ParameterNotNullAssertionBenchmark()
    val primeListBenchmark = PrimeListBenchmark()
    val stringBenchmark = StringBenchmark()
    val switchBenchmark = SwitchBenchmark()
    val withIndiciesBenchmark = WithIndiciesBenchmark()
    val callsBenchmark = CallsBenchmark()

    override val benchmarks = BenchmarksCollection(
            mutableMapOf(
                    "AbstractMethod.sortStrings" to abstractMethodBenchmark::sortStrings,
                    "AbstractMethod.sortStringsWithComparator" to abstractMethodBenchmark::sortStringsWithComparator,
                    "ClassArray.copy" to classArrayBenchmark::copy,
                    "ClassArray.copyManual" to classArrayBenchmark::copyManual,
                    "ClassArray.filterAndCount" to classArrayBenchmark::filterAndCount,
                    "ClassArray.filterAndMap" to classArrayBenchmark::filterAndMap,
                    "ClassArray.filterAndMapManual" to classArrayBenchmark::filterAndMapManual,
                    "ClassArray.filter" to classArrayBenchmark::filter,
                    "ClassArray.filterManual" to classArrayBenchmark::filterManual,
                    "ClassArray.countFilteredManual" to classArrayBenchmark::countFilteredManual,
                    "ClassArray.countFiltered" to classArrayBenchmark::countFiltered,
                    "ClassArray.countFilteredLocal" to classArrayBenchmark::countFilteredLocal,
                    "ClassBaseline.consume" to classBaselineBenchmark::consume,
                    "ClassBaseline.consumeField" to classBaselineBenchmark::consumeField,
                    "ClassBaseline.allocateList" to classBaselineBenchmark::allocateList,
                    "ClassBaseline.allocateArray" to classBaselineBenchmark::allocateArray,
                    "ClassBaseline.allocateListAndFill" to classBaselineBenchmark::allocateListAndFill,
                    "ClassBaseline.allocateListAndWrite" to classBaselineBenchmark::allocateListAndWrite,
                    "ClassBaseline.allocateArrayAndFill" to classBaselineBenchmark::allocateArrayAndFill,
                    "ClassList.copy" to classListBenchmark::copy,
                    "ClassList.copyManual" to classListBenchmark::copyManual,
                    "ClassList.filterAndCount" to classListBenchmark::filterAndCount,
                    "ClassList.filterAndCountWithLambda" to classListBenchmark::filterAndCountWithLambda,
                    "ClassList.filterWithLambda" to classListBenchmark::filterWithLambda,
                    "ClassList.mapWithLambda" to classListBenchmark::mapWithLambda,
                    "ClassList.countWithLambda" to classListBenchmark::countWithLambda,
                    "ClassList.filterAndMapWithLambda" to classListBenchmark::filterAndMapWithLambda,
                    "ClassList.filterAndMapWithLambdaAsSequence" to classListBenchmark::filterAndMapWithLambdaAsSequence,
                    "ClassList.filterAndMap" to classListBenchmark::filterAndMap,
                    "ClassList.filterAndMapManual" to classListBenchmark::filterAndMapManual,
                    "ClassList.filter" to classListBenchmark::filter,
                    "ClassList.filterManual" to classListBenchmark::filterManual,
                    "ClassList.countFilteredManual" to classListBenchmark::countFilteredManual,
                    "ClassList.countFiltered" to classListBenchmark::countFiltered,
                    "ClassList.reduce" to classListBenchmark::reduce,
                    "ClassStream.copy" to classStreamBenchmark::copy,
                    "ClassStream.copyManual" to classStreamBenchmark::copyManual,
                    "ClassStream.filterAndCount" to classStreamBenchmark::filterAndCount,
                    "ClassStream.filterAndMap" to classStreamBenchmark::filterAndMap,
                    "ClassStream.filterAndMapManual" to classStreamBenchmark::filterAndMapManual,
                    "ClassStream.filter" to classStreamBenchmark::filter,
                    "ClassStream.filterManual" to classStreamBenchmark::filterManual,
                    "ClassStream.countFilteredManual" to classStreamBenchmark::countFilteredManual,
                    "ClassStream.countFiltered" to classStreamBenchmark::countFiltered,
                    "ClassStream.reduce" to classStreamBenchmark::reduce,
                    "CompanionObject.invokeRegularFunction" to companionObjectBenchmark::invokeRegularFunction,
                    "CompanionObject.invokeJvmStaticFunction" to companionObjectBenchmark::invokeJvmStaticFunction,
                    "DefaultArgument.testOneOfTwo" to defaultArgumentBenchmark::testOneOfTwo,
                    "DefaultArgument.testTwoOfTwo" to defaultArgumentBenchmark::testTwoOfTwo,
                    "DefaultArgument.testOneOfFour" to defaultArgumentBenchmark::testOneOfFour,
                    "DefaultArgument.testFourOfFour" to defaultArgumentBenchmark::testFourOfFour,
                    "DefaultArgument.testOneOfEight" to defaultArgumentBenchmark::testOneOfEight,
                    "DefaultArgument.testEightOfEight" to defaultArgumentBenchmark::testEightOfEight,
                    "Elvis.testElvis" to elvisBenchmark::testElvis,
                    "Euler.problem1bySequence" to eulerBenchmark::problem1bySequence,
                    "Euler.problem1" to eulerBenchmark::problem1,
                    "Euler.problem2" to eulerBenchmark::problem2,
                    "Euler.problem4" to eulerBenchmark::problem4,
                    "Euler.problem8" to eulerBenchmark::problem8,
                    "Euler.problem9" to eulerBenchmark::problem9,
                    "Euler.problem14" to eulerBenchmark::problem14,
                    "Euler.problem14full" to eulerBenchmark::problem14full,
                    "Fibonacci.calcClassic" to fibonacciBenchmark::calcClassic,
                    "Fibonacci.calc" to fibonacciBenchmark::calc,
                    "Fibonacci.calcWithProgression" to fibonacciBenchmark::calcWithProgression,
                    "Fibonacci.calcSquare" to fibonacciBenchmark::calcSquare,
                    "ForLoops.arrayLoop" to forLoopsBenchmark::arrayLoop,
                    "ForLoops.intArrayLoop" to forLoopsBenchmark::intArrayLoop,
                    "ForLoops.floatArrayLoop" to forLoopsBenchmark::floatArrayLoop,
                    "ForLoops.charArrayLoop" to forLoopsBenchmark::charArrayLoop,
                    "ForLoops.stringLoop" to forLoopsBenchmark::stringLoop,
                    "ForLoops.arrayIndicesLoop" to forLoopsBenchmark::arrayIndicesLoop,
                    "ForLoops.intArrayIndicesLoop" to forLoopsBenchmark::intArrayIndicesLoop,
                    "ForLoops.floatArrayIndicesLoop" to forLoopsBenchmark::floatArrayIndicesLoop,
                    "ForLoops.charArrayIndicesLoop" to forLoopsBenchmark::charArrayIndicesLoop,
                    "ForLoops.stringIndicesLoop" to forLoopsBenchmark::stringIndicesLoop,
                    "Inline.calculate" to inlineBenchmark::calculate,
                    "Inline.calculateInline" to inlineBenchmark::calculateInline,
                    "Inline.calculateGeneric" to inlineBenchmark::calculateGeneric,
                    "Inline.calculateGenericInline" to inlineBenchmark::calculateGenericInline,
                    "IntArray.copy" to intArrayBenchmark::copy,
                    "IntArray.copyManual" to intArrayBenchmark::copyManual,
                    "IntArray.filterAndCount" to intArrayBenchmark::filterAndCount,
                    "IntArray.filterSomeAndCount" to intArrayBenchmark::filterSomeAndCount,
                    "IntArray.filterAndMap" to intArrayBenchmark::filterAndMap,
                    "IntArray.filterAndMapManual" to intArrayBenchmark::filterAndMapManual,
                    "IntArray.filter" to intArrayBenchmark::filter,
                    "IntArray.filterSome" to intArrayBenchmark::filterSome,
                    "IntArray.filterPrime" to intArrayBenchmark::filterPrime,
                    "IntArray.filterManual" to intArrayBenchmark::filterManual,
                    "IntArray.filterSomeManual" to intArrayBenchmark::filterSomeManual,
                    "IntArray.countFilteredManual" to intArrayBenchmark::countFilteredManual,
                    "IntArray.countFilteredSomeManual" to intArrayBenchmark::countFilteredSomeManual,
                    "IntArray.countFilteredPrimeManual" to intArrayBenchmark::countFilteredPrimeManual,
                    "IntArray.countFiltered" to intArrayBenchmark::countFiltered,
                    "IntArray.countFilteredSome" to intArrayBenchmark::countFilteredSome,
                    "IntArray.countFilteredPrime" to intArrayBenchmark::countFilteredPrime,
                    "IntArray.countFilteredLocal" to intArrayBenchmark::countFilteredLocal,
                    "IntArray.countFilteredSomeLocal" to intArrayBenchmark::countFilteredSomeLocal,
                    "IntArray.reduce" to intArrayBenchmark::reduce,
                    "IntBaseline.consume" to intBaselineBenchmark::consume,
                    "IntBaseline.allocateList" to intBaselineBenchmark::allocateList,
                    "IntBaseline.allocateArray" to intBaselineBenchmark::allocateArray,
                    "IntBaseline.allocateListAndFill" to intBaselineBenchmark::allocateListAndFill,
                    "IntBaseline.allocateArrayAndFill" to intBaselineBenchmark::allocateArrayAndFill,
                    "IntList.copy" to intListBenchmark::copy,
                    "IntList.copyManual" to intListBenchmark::copyManual,
                    "IntList.filterAndCount" to intListBenchmark::filterAndCount,
                    "IntList.filterAndMap" to intListBenchmark::filterAndMap,
                    "IntList.filterAndMapManual" to intListBenchmark::filterAndMapManual,
                    "IntList.filter" to intListBenchmark::filter,
                    "IntList.filterManual" to intListBenchmark::filterManual,
                    "IntList.countFilteredManual" to intListBenchmark::countFilteredManual,
                    "IntList.countFiltered" to intListBenchmark::countFiltered,
                    "IntList.countFilteredLocal" to intListBenchmark::countFilteredLocal,
                    "IntList.reduce" to intListBenchmark::reduce,
                    "IntStream.copy" to intStreamBenchmark::copy,
                    "IntStream.copyManual" to intStreamBenchmark::copyManual,
                    "IntStream.filterAndCount" to intStreamBenchmark::filterAndCount,
                    "IntStream.filterAndMap" to intStreamBenchmark::filterAndMap,
                    "IntStream.filterAndMapManual" to intStreamBenchmark::filterAndMapManual,
                    "IntStream.filter" to intStreamBenchmark::filter,
                    "IntStream.filterManual" to intStreamBenchmark::filterManual,
                    "IntStream.countFilteredManual" to intStreamBenchmark::countFilteredManual,
                    "IntStream.countFiltered" to intStreamBenchmark::countFiltered,
                    "IntStream.countFilteredLocal" to intStreamBenchmark::countFilteredLocal,
                    "IntStream.reduce" to intStreamBenchmark::reduce,
                    "Lambda.noncapturingLambda" to lambdaBenchmark::noncapturingLambda,
                    "Lambda.noncapturingLambdaNoInline" to lambdaBenchmark::noncapturingLambdaNoInline,
                    "Lambda.capturingLambda" to lambdaBenchmark::capturingLambda,
                    "Lambda.capturingLambdaNoInline" to lambdaBenchmark::capturingLambdaNoInline,
                    "Lambda.mutatingLambda" to lambdaBenchmark::mutatingLambda,
                    "Lambda.mutatingLambdaNoInline" to lambdaBenchmark::mutatingLambdaNoInline,
                    "Lambda.methodReference" to lambdaBenchmark::methodReference,
                    "Lambda.methodReferenceNoInline" to lambdaBenchmark::methodReferenceNoInline,
                    "Loop.arrayLoop" to loopBenchmark::arrayLoop,
                    "Loop.arrayIndexLoop" to loopBenchmark::arrayIndexLoop,
                    "Loop.rangeLoop" to loopBenchmark::rangeLoop,
                    "Loop.arrayListLoop" to loopBenchmark::arrayListLoop,
                    "Loop.arrayWhileLoop" to loopBenchmark::arrayWhileLoop,
                    "Loop.arrayForeachLoop" to loopBenchmark::arrayForeachLoop,
                    "Loop.arrayListForeachLoop" to loopBenchmark::arrayListForeachLoop,
                    "MatrixMap.add" to matrixMapBenchmark::add,
                    "ParameterNotNull.invokeOneArgWithNullCheck" to parameterNotNullAssertionBenchmark::invokeOneArgWithNullCheck,
                    "ParameterNotNull.invokeOneArgWithoutNullCheck" to parameterNotNullAssertionBenchmark::invokeOneArgWithoutNullCheck,
                    "ParameterNotNull.invokeTwoArgsWithNullCheck" to parameterNotNullAssertionBenchmark::invokeTwoArgsWithNullCheck,
                    "ParameterNotNull.invokeTwoArgsWithoutNullCheck" to parameterNotNullAssertionBenchmark::invokeTwoArgsWithoutNullCheck,
                    "ParameterNotNull.invokeEightArgsWithNullCheck" to parameterNotNullAssertionBenchmark::invokeEightArgsWithNullCheck,
                    "ParameterNotNull.invokeEightArgsWithoutNullCheck" to parameterNotNullAssertionBenchmark::invokeEightArgsWithoutNullCheck,
                    "PrimeList.calcDirect" to primeListBenchmark::calcDirect,
                    "PrimeList.calcEratosthenes" to primeListBenchmark::calcEratosthenes,
                    "String.stringConcat" to stringBenchmark::stringConcat,
                    "String.stringConcatNullable" to stringBenchmark::stringConcatNullable,
                    "String.stringBuilderConcat" to stringBenchmark::stringBuilderConcat,
                    "String.stringBuilderConcatNullable" to stringBenchmark::stringBuilderConcatNullable,
                    "String.summarizeSplittedCsv" to stringBenchmark::summarizeSplittedCsv,
                    "Switch.testSparseIntSwitch" to switchBenchmark::testSparseIntSwitch,
                    "Switch.testDenseIntSwitch" to switchBenchmark::testDenseIntSwitch,
                    "Switch.testConstSwitch" to switchBenchmark::testConstSwitch,
                    "Switch.testObjConstSwitch" to switchBenchmark::testObjConstSwitch,
                    "Switch.testVarSwitch" to switchBenchmark::testVarSwitch,
                    "Switch.testStringsSwitch" to switchBenchmark::testStringsSwitch,
                    "Switch.testEnumsSwitch" to switchBenchmark::testEnumsSwitch,
                    "Switch.testDenseEnumsSwitch" to switchBenchmark::testDenseEnumsSwitch,
                    "Switch.testSealedWhenSwitch" to switchBenchmark::testSealedWhenSwitch,
                    "WithIndicies.withIndicies" to withIndiciesBenchmark::withIndicies,
                    "WithIndicies.withIndiciesManual" to withIndiciesBenchmark::withIndiciesManual,
                    "OctoTest" to ::octoTest,
                    "Calls.finalMethod" to callsBenchmark::finalMethodCall,
                    "Calls.openMethodMonomorphic" to callsBenchmark::classOpenMethodCall_MonomorphicCallsite,
                    "Calls.openMethodBimorphic" to callsBenchmark::classOpenMethodCall_BimorphicCallsite,
                    "Calls.openMethodTrimorphic" to callsBenchmark::classOpenMethodCall_TrimorphicCallsite,
                    "Calls.interfaceMethodMonomorphic" to callsBenchmark::interfaceMethodCall_MonomorphicCallsite,
                    "Calls.interfaceMethodBimorphic" to callsBenchmark::interfaceMethodCall_BimorphicCallsite,
                    "Calls.interfaceMethodTrimorphic" to callsBenchmark::interfaceMethodCall_TrimorphicCallsite,
                    "Calls.returnBoxUnboxFolding" to callsBenchmark::returnBoxUnboxFolding,
                    "Calls.parameterBoxUnboxFolding" to callsBenchmark::parameterBoxUnboxFolding
            )
    )
}

fun main(args: Array<String>) {
    BenchmarksRunner.runBenchmarks(args, { parser: ArgParser ->
        RingLauncher(parser.get<Int>("warmup")!!, parser.get<Int>("repeat")!!, parser.get<String>("prefix")!!)
                .launch(parser.getAll<String>("filter"), parser.getAll<String>("filterRegex"))
    })
}