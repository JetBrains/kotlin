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
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import octoTest
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.ring.*

@State(Scope.Benchmark)
class AbstractMethod : SkipWhenBaseOnly() {
    val instance = AbstractMethodBenchmark()

    @Benchmark
    fun sortStrings() {
        instance.sortStrings()
    }

    @Benchmark
    fun sortStringsWithComparator() {
        skipWhenBaseOnly()
        instance.sortStrings()
    }
}

@State(Scope.Benchmark)
class AllocationBenchmark : SkipWhenBaseOnly() {
    val instance = org.jetbrains.ring.AllocationBenchmark()

    @Benchmark
    fun allocateObjects() {
        skipWhenBaseOnly()
        instance.allocateObjects()
    }
}

@State(Scope.Benchmark)
class ArrayCopyBenchmark {
    val instance = org.jetbrains.ring.ArrayCopyBenchmark()

    @Benchmark
    fun copyInSameArray() {
        instance.copyInSameArray()
    }
}

@State(Scope.Benchmark)
class BunnymarkBenchmark {
    val instance = org.jetbrains.ring.BunnymarkBenchmark()

    @Benchmark
    fun testBunnymark() {
        instance.testBunnymark()
    }
}

@State(Scope.Benchmark)
class Calls : SkipWhenBaseOnly() {
    val instance = CallsBenchmark()

    @Benchmark
    fun finalMethod() {
        instance.finalMethodCall()
    }

    @Benchmark
    fun interfaceMethodMonomorphic() {
        instance.interfaceMethodCall_MonomorphicCallsite()
    }

    @Benchmark
    fun openMethodMonomorphic() {
        instance.classOpenMethodCall_MonomorphicCallsite()
    }

    @Benchmark
    fun parameterBoxUnboxFolding() {
        instance.parameterBoxUnboxFolding()
    }

    @Benchmark
    fun returnBoxUnboxFolding() {
        instance.returnBoxUnboxFolding()
    }

    @Benchmark
    fun interfaceMethodBimorphic() {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_BimorphicCallsite()
    }

    @Benchmark
    fun interfaceMethodHexamorphic() {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_HexamorphicCallsite()
    }

    @Benchmark
    fun interfaceMethodTrimorphic() {
        skipWhenBaseOnly()
        instance.interfaceMethodCall_TrimorphicCallsite()
    }

    @Benchmark
    fun openMethodBimorphic() {
        skipWhenBaseOnly()
        instance.classOpenMethodCall_BimorphicCallsite()
    }

    @Benchmark
    fun openMethodTrimorphic() {
        skipWhenBaseOnly()
        instance.classOpenMethodCall_TrimorphicCallsite()
    }
}

@State(Scope.Benchmark)
class Casts {
    val instance = CastsBenchmark()

    @Benchmark
    fun classCast() {
        instance.classCast()
    }

    @Benchmark
    fun interfaceCast() {
        instance.interfaceCast()
    }
}

@State(Scope.Benchmark)
class ChainableBenchmark {
    val instance = org.jetbrains.ring.ChainableBenchmark()

    @Benchmark
    fun testChainable() {
        instance.testChainable()
    }
}

@State(Scope.Benchmark)
class ClassArray : SkipWhenBaseOnly() {
    val instance = ClassArrayBenchmark()

    @Benchmark
    fun copy() {
        instance.copy()
    }

    @Benchmark
    fun filter() {
        instance.filter()
    }

    @Benchmark
    fun countFiltered() {
        instance.countFiltered()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFilteredLocal() {
        skipWhenBaseOnly()
        instance.countFilteredLocal()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }
}

@State(Scope.Benchmark)
class ClassBaseline : SkipWhenBaseOnly() {
    val instance = ClassBaselineBenchmark()

    @Benchmark
    fun consume() {
        instance.consume()
    }

    @Benchmark
    fun consumeField() {
        instance.consumeField()
    }

    @Benchmark
    fun allocateListAndFill() {
        instance.allocateListAndFill()
    }

    @Benchmark
    fun allocateArray() {
        skipWhenBaseOnly()
        instance.allocateArray()
    }

    @Benchmark
    fun allocateArrayAndFill() {
        skipWhenBaseOnly()
        instance.allocateArrayAndFill()
    }

    @Benchmark
    fun allocateList() {
        skipWhenBaseOnly()
        instance.allocateList()
    }

    @Benchmark
    fun allocateListAndWrite() {
        skipWhenBaseOnly()
        instance.allocateListAndWrite()
    }
}

@State(Scope.Benchmark)
class ClassList : SkipWhenBaseOnly() {
    val instance = ClassListBenchmark()

    @Benchmark
    fun copy() {
        instance.copy()
    }

    @Benchmark
    fun mapWithLambda() {
        instance.mapWithLambda()
    }

    @Benchmark
    fun filter() {
        instance.filter()
    }

    @Benchmark
    fun reduce() {
        instance.reduce()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun countWithLambda() {
        skipWhenBaseOnly()
        instance.countWithLambda()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndCountWithLambda() {
        skipWhenBaseOnly()
        instance.filterAndCountWithLambda()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterAndMapWithLambda() {
        skipWhenBaseOnly()
        instance.filterAndMapWithLambda()
    }

    @Benchmark
    fun filterAndMapWithLambdaAsSequence() {
        skipWhenBaseOnly()
        instance.filterAndMapWithLambdaAsSequence()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }

    @Benchmark
    fun filterWithLambda() {
        skipWhenBaseOnly()
        instance.filterWithLambda()
    }
}

@State(Scope.Benchmark)
class ClassStream : SkipWhenBaseOnly() {
    val instance = ClassStreamBenchmark()

    @Benchmark
    fun copy() {
        instance.copy()
    }

    @Benchmark
    fun filter() {
        instance.filter()
    }

    @Benchmark
    fun reduce() {
        instance.reduce()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }
}

@State(Scope.Benchmark)
class CompanionObject : SkipWhenBaseOnly() {
    val instance = CompanionObjectBenchmark()

    @Benchmark
    fun invokeRegularFunction() {
        skipWhenBaseOnly()
        instance.invokeRegularFunction()
    }
}

@State(Scope.Benchmark)
class ComplexArrays : SkipWhenBaseOnly() {
    val instance = ComplexArraysBenchmark()

    @Benchmark
    fun outerProduct() {
        skipWhenBaseOnly()
        instance.outerProduct()
    }
}

@State(Scope.Benchmark)
class CoordinatesSolver {
    val instance = CoordinatesSolverBenchmark()

    @Benchmark
    fun solve() {
        instance.solve()
    }
}

@State(Scope.Benchmark)
class DefaultArgument : SkipWhenBaseOnly() {
    val instance = DefaultArgumentBenchmark()

    @Benchmark
    fun testEightOfEight() {
        skipWhenBaseOnly()
        instance.testEightOfEight()
    }

    @Benchmark
    fun testFourOfFour() {
        skipWhenBaseOnly()
        instance.testFourOfFour()
    }

    @Benchmark
    fun testOneOfEight() {
        skipWhenBaseOnly()
        instance.testOneOfEight()
    }

    @Benchmark
    fun testOneOfFour() {
        skipWhenBaseOnly()
        instance.testOneOfFour()
    }

    @Benchmark
    fun testOneOfTwo() {
        skipWhenBaseOnly()
        instance.testOneOfTwo()
    }

    @Benchmark
    fun testTwoOfTwo() {
        skipWhenBaseOnly()
        instance.testTwoOfTwo()
    }
}

@State(Scope.Benchmark)
class DeltaBlueHideName {
    val instance = DeltaBlueBenchmark()

    @Benchmark
    fun DeltaBlue() {
        instance.deltaBlue()
    }
}

@State(Scope.Benchmark)
class Elvis : SkipWhenBaseOnly() {
    val instance = ElvisBenchmark()

    @Benchmark
    fun testElvis() {
        instance.testElvis()
    }

    @Benchmark
    fun testCompositeElvis() {
        skipWhenBaseOnly()
        instance.testCompositeElvis()
    }
}

@State(Scope.Benchmark)
class Euler : SkipWhenBaseOnly() {
    val instance = EulerBenchmark()

    @Benchmark
    fun problem14() {
        instance.problem14()
    }

    @Benchmark
    fun problem1bySequence() {
        instance.problem1bySequence()
    }

    @Benchmark
    fun problem9() {
        instance.problem9()
    }

    @Benchmark
    fun problem1() {
        skipWhenBaseOnly()
        instance.problem1()
    }

    @Benchmark
    fun problem14full() {
        skipWhenBaseOnly()
        instance.problem14full()
    }

    @Benchmark
    fun problem2() {
        skipWhenBaseOnly()
        instance.problem2()
    }

    @Benchmark
    fun problem4() {
        skipWhenBaseOnly()
        instance.problem4()
    }

    @Benchmark
    fun problem8() {
        skipWhenBaseOnly()
        instance.problem8()
    }
}

@State(Scope.Benchmark)
class Fibonacci : SkipWhenBaseOnly() {
    val instance = FibonacciBenchmark()

    @Benchmark
    fun calc() {
        instance.calc()
    }

    @Benchmark
    fun calcClassic() {
        instance.calcClassic()
    }

    @Benchmark
    fun calcSquare() {
        skipWhenBaseOnly()
        instance.calcSquare()
    }

    @Benchmark
    fun calcWithProgression() {
        skipWhenBaseOnly()
        instance.calcWithProgression()
    }
}

@State(Scope.Benchmark)
class ForLoops : SkipWhenBaseOnly() {
    val instance = ForLoopsBenchmark()

    @Benchmark
    fun arrayIndicesLoop() {
        instance.arrayIndicesLoop()
    }

    @Benchmark
    fun arrayLoop() {
        instance.arrayLoop()
    }

    @Benchmark
    fun charArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.charArrayIndicesLoop()
    }

    @Benchmark
    fun charArrayLoop() {
        skipWhenBaseOnly()
        instance.charArrayLoop()
    }

    @Benchmark
    fun floatArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.floatArrayIndicesLoop()
    }

    @Benchmark
    fun floatArrayLoop() {
        skipWhenBaseOnly()
        instance.floatArrayLoop()
    }

    @Benchmark
    fun intArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.intArrayIndicesLoop()
    }

    @Benchmark
    fun intArrayLoop() {
        skipWhenBaseOnly()
        instance.intArrayLoop()
    }

    @Benchmark
    fun stringArrayLoop() {
        skipWhenBaseOnly()
        instance.stringArrayLoop()
    }

    @Benchmark
    fun stringIndicesLoop() {
        skipWhenBaseOnly()
        instance.stringIndicesLoop()
    }

    @Benchmark
    fun stringLoop() {
        skipWhenBaseOnly()
        instance.stringLoop()
    }

    @Benchmark
    fun uIntArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.uIntArrayIndicesLoop()
    }

    @Benchmark
    fun uIntArrayLoop() {
        skipWhenBaseOnly()
        instance.uIntArrayLoop()
    }

    @Benchmark
    fun uLongArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.uLongArrayIndicesLoop()
    }

    @Benchmark
    fun uLongArrayLoop() {
        skipWhenBaseOnly()
        instance.uLongArrayLoop()
    }

    @Benchmark
    fun uShortArrayIndicesLoop() {
        skipWhenBaseOnly()
        instance.uShortArrayIndicesLoop()
    }

    @Benchmark
    fun uShortArrayLoop() {
        skipWhenBaseOnly()
        instance.uShortArrayLoop()
    }
}

@State(Scope.Benchmark)
class GenericArrayView : SkipWhenBaseOnly() {
    val instance = GenericArrayViewBenchmark()

    @Benchmark
    fun inlined() {
        skipWhenBaseOnly()
        instance.inlined()
    }

    @Benchmark
    fun manual() {
        skipWhenBaseOnly()
        instance.manual()
    }

    @Benchmark
    fun origin() {
        skipWhenBaseOnly()
        instance.origin()
    }

    @Benchmark
    fun specialized() {
        skipWhenBaseOnly()
        instance.specialized()
    }
}

@State(Scope.Benchmark)
class GraphSolver {
    val instance = GraphSolverBenchmark()

    @Benchmark
    fun solve() {
        instance.solve()
    }
}


@State(Scope.Benchmark)
class Inheritance {
    val instance = InheritanceBenchmark()

    @Benchmark
    fun baseCalls() {
        instance.baseCalls()
    }
}


@State(Scope.Benchmark)
class Inline : SkipWhenBaseOnly() {
    val instance = InlineBenchmark()

    @Benchmark
    fun calculateInline() {
        instance.calculateInline()
    }

    @Benchmark
    fun calculate() {
        skipWhenBaseOnly()
        instance.calculate()
    }

    @Benchmark
    fun calculateGeneric() {
        skipWhenBaseOnly()
        instance.calculateGeneric()
    }

    @Benchmark
    fun calculateGenericInline() {
        skipWhenBaseOnly()
        instance.calculateGenericInline()
    }
}


@State(Scope.Benchmark)
class IntArray : SkipWhenBaseOnly() {
    val instance = IntArrayBenchmark()

    @Benchmark
    fun copy() {
        skipWhenBaseOnly()
        instance.copy()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredLocal() {
        skipWhenBaseOnly()
        instance.countFilteredLocal()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun countFilteredPrime() {
        skipWhenBaseOnly()
        instance.countFilteredPrime()
    }

    @Benchmark
    fun countFilteredPrimeManual() {
        skipWhenBaseOnly()
        instance.countFilteredPrimeManual()
    }

    @Benchmark
    fun countFilteredSome() {
        skipWhenBaseOnly()
        instance.countFilteredSome()
    }

    @Benchmark
    fun countFilteredSomeLocal() {
        skipWhenBaseOnly()
        instance.countFilteredSomeLocal()
    }

    @Benchmark
    fun countFilteredSomeManual() {
        skipWhenBaseOnly()
        instance.countFilteredSomeManual()
    }

    @Benchmark
    fun filter() {
        skipWhenBaseOnly()
        instance.filter()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }

    @Benchmark
    fun filterPrime() {
        skipWhenBaseOnly()
        instance.filterPrime()
    }

    @Benchmark
    fun filterSome() {
        skipWhenBaseOnly()
        instance.filterSome()
    }

    @Benchmark
    fun filterSomeAndCount() {
        skipWhenBaseOnly()
        instance.filterSomeAndCount()
    }

    @Benchmark
    fun filterSomeManual() {
        skipWhenBaseOnly()
        instance.filterSomeManual()
    }

    @Benchmark
    fun reduce() {
        skipWhenBaseOnly()
        instance.reduce()
    }
}


@State(Scope.Benchmark)
class IntBaseline : SkipWhenBaseOnly() {
    val instance = IntBaselineBenchmark()

    @Benchmark
    fun allocateArray() {
        skipWhenBaseOnly()
        instance.allocateArray()
    }

    @Benchmark
    fun allocateArrayAndFill() {
        skipWhenBaseOnly()
        instance.allocateArrayAndFill()
    }

    @Benchmark
    fun allocateList() {
        skipWhenBaseOnly()
        instance.allocateList()
    }

    @Benchmark
    fun allocateListAndFill() {
        skipWhenBaseOnly()
        instance.allocateListAndFill()
    }

    @Benchmark
    fun consume() {
        skipWhenBaseOnly()
        instance.consume()
    }
}


@State(Scope.Benchmark)
class IntList : SkipWhenBaseOnly() {
    val instance = IntListBenchmark()

    @Benchmark
    fun copy() {
        skipWhenBaseOnly()
        instance.copy()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredLocal() {
        skipWhenBaseOnly()
        instance.countFilteredLocal()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun filter() {
        skipWhenBaseOnly()
        instance.filter()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }

    @Benchmark
    fun reduce() {
        skipWhenBaseOnly()
        instance.reduce()
    }
}


@State(Scope.Benchmark)
class IntStream : SkipWhenBaseOnly() {
    val instance = IntStreamBenchmark()

    @Benchmark
    fun copy() {
        skipWhenBaseOnly()
        instance.copy()
    }

    @Benchmark
    fun copyManual() {
        skipWhenBaseOnly()
        instance.copyManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredLocal() {
        skipWhenBaseOnly()
        instance.countFilteredLocal()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun filter() {
        skipWhenBaseOnly()
        instance.filter()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndMap() {
        skipWhenBaseOnly()
        instance.filterAndMap()
    }

    @Benchmark
    fun filterAndMapManual() {
        skipWhenBaseOnly()
        instance.filterAndMapManual()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }

    @Benchmark
    fun reduce() {
        skipWhenBaseOnly()
        instance.reduce()
    }
}


@State(Scope.Benchmark)
class Iterator : SkipWhenBaseOnly() {
    val instance = IteratorBenchmark()

    @Benchmark
    fun abstractIterable() {
        skipWhenBaseOnly()
        instance.abstractIterable()
    }

    @Benchmark
    fun baseline() {
        skipWhenBaseOnly()
        instance.baseline()
    }

    @Benchmark
    fun concreteIterable() {
        skipWhenBaseOnly()
        instance.concreteIterable()
    }
}

@State(Scope.Benchmark)
class Lambda : SkipWhenBaseOnly() {
    val instance = LambdaBenchmark()

    @Benchmark
    fun capturingLambda() {
        instance.capturingLambda()
    }

    @Benchmark
    fun methodReference() {
        instance.methodReference()
    }

    @Benchmark
    fun noncapturingLambda() {
        instance.noncapturingLambda()
    }

    @Benchmark
    fun capturingLambdaNoInline() {
        skipWhenBaseOnly()
        instance.capturingLambdaNoInline()
    }

    @Benchmark
    fun methodReferenceNoInline() {
        skipWhenBaseOnly()
        instance.methodReferenceNoInline()
    }

    @Benchmark
    fun mutatingLambda() {
        skipWhenBaseOnly()
        instance.mutatingLambda()
    }

    @Benchmark
    fun mutatingLambdaNoInline() {
        skipWhenBaseOnly()
        instance.mutatingLambdaNoInline()
    }

    @Benchmark
    fun noncapturingLambdaNoInline() {
        skipWhenBaseOnly()
        instance.noncapturingLambdaNoInline()
    }
}


@State(Scope.Benchmark)
class LifeHideName : SkipWhenBaseOnly() {
    val instance = LifeBenchmark()

    @Benchmark
    fun Life() {
        skipWhenBaseOnly()
        instance.bench()
    }
}


@State(Scope.Benchmark)
class LifeWithMarkHelpersHideName : SkipWhenBaseOnly() {
    val instance = LifeWithMarkHelpersBenchmark()

    @Benchmark
    fun LifeWithMarkHelpers() {
        skipWhenBaseOnly()
        instance.bench()
    }

    @TearDown
    fun cleanup() {
        instance.terminate()
    }
}


@State(Scope.Benchmark)
class LinkedListWithAtomicsBenchmarkHideName {
    val instance = org.jetbrains.ring.LinkedListWithAtomicsBenchmark()

    @Benchmark
    fun LinkedListWithAtomicsBenchmark() {
        instance.ensureNext()
    }

}

@State(Scope.Benchmark)
class LocalObjects : SkipWhenBaseOnly() {
    val instance = LocalObjectsBenchmark()

    @Benchmark
    fun localArray() {
        skipWhenBaseOnly()
        instance.localArray()
    }
}


@State(Scope.Benchmark)
class Loop : SkipWhenBaseOnly() {
    val instance = LoopBenchmark()

    @Benchmark
    fun arrayForeachLoop() {
        instance.arrayForeachLoop()
    }

    @Benchmark
    fun arrayListForeachLoop() {
        instance.arrayListForeachLoop()
    }

    @Benchmark
    fun arrayLoop() {
        instance.arrayLoop()
    }

    @Benchmark
    fun arrayWhileLoop() {
        instance.arrayWhileLoop()
    }

    @Benchmark
    fun rangeLoop() {
        instance.rangeLoop()
    }

    @Benchmark
    fun arrayIndexLoop() {
        skipWhenBaseOnly()
        instance.arrayIndexLoop()
    }

    @Benchmark
    fun arrayListLoop() {
        skipWhenBaseOnly()
        instance.arrayListLoop()
    }
}


@State(Scope.Benchmark)
class MatrixMap {
    val instance = MatrixMapBenchmark()

    @Benchmark
    fun add() {
        instance.add()
    }
}


@State(Scope.Benchmark)
class OctoTestHideName {
    @Benchmark
    fun OctoTest() {
        octoTest()
    }
}


@State(Scope.Benchmark)
class ParameterNotNull : SkipWhenBaseOnly() {
    val instance = ParameterNotNullAssertionBenchmark()

    @Benchmark
    fun invokeEightArgsWithNullCheck() {
        skipWhenBaseOnly()
        instance.invokeEightArgsWithNullCheck()
    }

    @Benchmark
    fun invokeEightArgsWithoutNullCheck() {
        skipWhenBaseOnly()
        instance.invokeEightArgsWithoutNullCheck()
    }

    @Benchmark
    fun invokeOneArgWithNullCheck() {
        skipWhenBaseOnly()
        instance.invokeOneArgWithNullCheck()
    }

    @Benchmark
    fun invokeOneArgWithoutNullCheck() {
        skipWhenBaseOnly()
        instance.invokeOneArgWithoutNullCheck()
    }

    @Benchmark
    fun invokeTwoArgsWithNullCheck() {
        skipWhenBaseOnly()
        instance.invokeTwoArgsWithNullCheck()
    }

    @Benchmark
    fun invokeTwoArgsWithoutNullCheck() {
        skipWhenBaseOnly()
        instance.invokeTwoArgsWithoutNullCheck()
    }
}


@State(Scope.Benchmark)
class PrimeList {
    val instance = PrimeListBenchmark()

    @Benchmark
    fun calcDirect() {
        instance.calcDirect()
    }

    @Benchmark
    fun calcEratosthenes() {
        instance.calcEratosthenes()
    }
}


@State(Scope.Benchmark)
class RichardsHideName {
    val instance = RichardsBenchmark()

    @Benchmark
    fun Richards() {
        instance.runRichards()
    }

}

@State(Scope.Benchmark)
class Singleton {
    val instance = SingletonBenchmark()

    @Benchmark
    fun access() {
        instance.access()
    }

}

@State(Scope.Benchmark)
class SplayHideName {
    val instance = SplayBenchmark()

    @Benchmark
    fun Splay() {
        instance.runSplay()
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDown()
    }
}

@State(Scope.Benchmark)
class SplayWithMarkHelpersHideName {
    val instance = SplayBenchmarkWithMarkHelpers()

    @Benchmark
    fun SplayWithMarkHelpers() {
        instance.runSplayWithMarkHelpers()
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDownMarkHelpers()
    }
}

@State(Scope.Benchmark)
class SplayWithWorkersHideName {
    val instance = SplayBenchmarkUsingWorkers()

    @Benchmark
    fun SplayWithWorkers() {
        instance.runSplayWorkers()
    }

    @TearDown
    fun cleanup() {
        instance.splayTearDownWorkers()
    }
}

@State(Scope.Benchmark)
class String : SkipWhenBaseOnly() {
    val instance = StringBenchmark()

    @Benchmark
    fun stringBuilderConcat() {
        instance.stringBuilderConcat()
    }

    @Benchmark
    fun stringBuilderConcatNullable() {
        instance.stringBuilderConcatNullable()
    }

    @Benchmark
    fun stringConcat() {
        instance.stringConcat()
    }

    @Benchmark
    fun summarizeSplittedCsv() {
        instance.summarizeSplittedCsv()
    }

    @Benchmark
    fun stringConcatNullable() {
        skipWhenBaseOnly()
        instance.stringConcatNullable()
    }
}


@State(Scope.Benchmark)
class SubList : SkipWhenBaseOnly() {
    val instance = SubListBenchmark()

    @Benchmark
    fun concatenate() {
        skipWhenBaseOnly()
        instance.concatenate()
    }

    @Benchmark
    fun concatenateManual() {
        skipWhenBaseOnly()
        instance.concatenateManual()
    }

    @Benchmark
    fun countFiltered() {
        skipWhenBaseOnly()
        instance.countFiltered()
    }

    @Benchmark
    fun countFilteredManual() {
        skipWhenBaseOnly()
        instance.countFilteredManual()
    }

    @Benchmark
    fun countWithLambda() {
        skipWhenBaseOnly()
        instance.countWithLambda()
    }

    @Benchmark
    fun filterAndCount() {
        skipWhenBaseOnly()
        instance.filterAndCount()
    }

    @Benchmark
    fun filterAndCountWithLambda() {
        skipWhenBaseOnly()
        instance.filterAndCountWithLambda()
    }

    @Benchmark
    fun filterManual() {
        skipWhenBaseOnly()
        instance.filterManual()
    }

    @Benchmark
    fun reduce() {
        skipWhenBaseOnly()
        instance.reduce()
    }
}


@State(Scope.Benchmark)
class Switch : SkipWhenBaseOnly() {
    val instance = SwitchBenchmark()

    @Benchmark
    fun testEnumsSwitch() {
        instance.testEnumsSwitch()
    }

    @Benchmark
    fun testSealedWhenSwitch() {
        instance.testSealedWhenSwitch()
    }

    @Benchmark
    fun testStringsSwitch() {
        instance.testStringsSwitch()
    }

    @Benchmark
    fun testConstSwitch() {
        skipWhenBaseOnly()
        instance.testConstSwitch()
    }

    @Benchmark
    fun testDenseEnumsSwitch() {
        skipWhenBaseOnly()
        instance.testDenseEnumsSwitch()
    }

    @Benchmark
    fun testDenseIntSwitch() {
        skipWhenBaseOnly()
        instance.testDenseIntSwitch()
    }

    @Benchmark
    fun testObjConstSwitch() {
        skipWhenBaseOnly()
        instance.testObjConstSwitch()
    }

    @Benchmark
    fun testSparseIntSwitch() {
        skipWhenBaseOnly()
        instance.testSparseIntSwitch()
    }

    @Benchmark
    fun testVarSwitch() {
        skipWhenBaseOnly()
        instance.testVarSwitch()
    }
}


@State(Scope.Benchmark)
class WeakRefBenchmark : SkipWhenBaseOnly() {
    val instance = org.jetbrains.ring.WeakRefBenchmark()

    @Benchmark
    fun aliveReference() {
        skipWhenBaseOnly()
        instance.aliveReference()
    }

    @Benchmark
    fun deadReference() {
        skipWhenBaseOnly()
        instance.deadReference()
    }

    @Benchmark
    fun dyingReference() {
        skipWhenBaseOnly()
        instance.dyingReference()
    }

    @TearDown
    fun cleanup() {
        instance.clean()
    }
}

@State(Scope.Benchmark)
class WithIndicies : SkipWhenBaseOnly() {
    val instance = WithIndiciesBenchmark()

    @Benchmark
    fun withIndicies() {
        instance.withIndicies()
    }

    @Benchmark
    fun withIndiciesManual() {
        skipWhenBaseOnly()
        instance.withIndiciesManual()
    }
}
