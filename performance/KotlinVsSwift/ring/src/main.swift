/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

var runner = BenchmarksRunner()
let args = KotlinArray(size: Int32(CommandLine.arguments.count - 1), init: {index in
    CommandLine.arguments[Int(truncating: index) + 1]
})

let companion = BenchmarkEntryWithInit.Companion()

var swiftLauncher = SwiftLauncher()

extension SwiftLauncher {
    static func abstractMethodBenchmark(_ instance: Any) -> AbstractMethodBenchmark {
        return instance as! AbstractMethodBenchmark
    }

    static func classArrayBenchmark(_ instance: Any) -> ClassArrayBenchmark {
        return instance as! ClassArrayBenchmark
    }

    static func classListBenchmark(_ instance: Any) -> ClassListBenchmark {
        return instance as! ClassListBenchmark
    }

    static func classBaselineBenchmark(_ instance: Any) -> ClassBaselineBenchmark {
        return instance as! ClassBaselineBenchmark
    }

    static func classStreamBenchmark(_ instance: Any) -> ClassStreamBenchmark {
        return instance as! ClassStreamBenchmark
    }

    static func companionObjectBenchmark(_ instance: Any) -> CompanionObjectBenchmark {
        return instance as! CompanionObjectBenchmark
    }

    static func defaultArgumentBenchmark(_ instance: Any) -> DefaultArgumentBenchmark {
        return instance as! DefaultArgumentBenchmark
    }

    static func elvisBenchmark(_ instance: Any) -> ElvisBenchmark {
        return instance as! ElvisBenchmark
    }

    static func eulerBenchmark(_ instance: Any) -> EulerBenchmark {
        return instance as! EulerBenchmark
    }

    static func fibonacciBenchmark(_ instance: Any) -> FibonacciBenchmark {
        return instance as! FibonacciBenchmark
    }

    static func forLoopsBenchmark(_ instance: Any) -> ForLoopsBenchmark {
        return instance as! ForLoopsBenchmark
    }

    static func inlineBenchmark(_ instance: Any) -> InlineBenchmark {
        return instance as! InlineBenchmark
    }

    static func intArrayBenchmark(_ instance: Any) -> IntArrayBenchmark {
        return instance as! IntArrayBenchmark
    }

    static func intBaselineBenchmark(_ instance: Any) -> IntBaselineBenchmark {
        return instance as! IntBaselineBenchmark
    }

    static func intStreamBenchmark(_ instance: Any) -> IntStreamBenchmark {
        return instance as! IntStreamBenchmark
    }

    static func lambdaBenchmark(_ instance: Any) -> LambdaBenchmark {
        return instance as! LambdaBenchmark
    }

    static func loopBenchmark(_ instance: Any) -> LoopBenchmark {
        return instance as! LoopBenchmark
    }

    static func matrixMapBenchmark(_ instance: Any) -> MatrixMapBenchmark {
        return instance as! MatrixMapBenchmark
    }

    static func parameterNotNullAssertionBenchmark(_ instance: Any) -> ParameterNotNullAssertionBenchmark {
        return instance as! ParameterNotNullAssertionBenchmark
    }

    static func primeListBenchmark(_ instance: Any) -> PrimeListBenchmark {
        return instance as! PrimeListBenchmark
    }

    static func stringBenchmark(_ instance: Any) -> StringBenchmark {
        return instance as! StringBenchmark
    }

    static func switchBenchmark(_ instance: Any) -> SwitchBenchmark {
        return instance as! SwitchBenchmark
    }

    static func withIndiciesBenchmark(_ instance: Any) -> WithIndiciesBenchmark {
        return instance as! WithIndiciesBenchmark
    }

    static func callsBenchmarks(_ instance: Any) -> CallsBenchmarks {
        return instance as! CallsBenchmarks
    }

    static func coordinatesSolverBenchmark(_ instance: Any) -> CoordinatesSolverBenchmark {
        return instance as! CoordinatesSolverBenchmark
    }

    static func graphSolverBenchmark(_ instance: Any) -> GraphSolverBenchmark {
        return instance as! GraphSolverBenchmark
    }

    static func castsBenchmark(_ instance: Any) -> CastsBenchmark {
        return instance as! CastsBenchmark
    }
}

swiftLauncher.add(name: "AbstractMethod.sortStrings", benchmark: companion.create(ctor: { return AbstractMethodBenchmark() },
        lambda: { SwiftLauncher.abstractMethodBenchmark($0).sortStrings() }))
swiftLauncher.add(name: "AbstractMethod.sortStringsWithComparator", benchmark: companion.create(ctor: { return AbstractMethodBenchmark() },
        lambda: { SwiftLauncher.abstractMethodBenchmark($0).sortStringsWithComparator() }))

swiftLauncher.add(name: "ClassArray.copy", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).copy() }))
swiftLauncher.add(name: "ClassArray.copyManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).copyManual() }))
swiftLauncher.add(name: "ClassArray.filterAndCount", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).filterAndCount() }))
swiftLauncher.add(name: "ClassArray.filterAndMap", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).filterAndMap() }))
swiftLauncher.add(name: "ClassArray.filterAndMapManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).filterAndMapManual() }))
swiftLauncher.add(name: "ClassArray.filter", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).filter() }))
swiftLauncher.add(name: "ClassArray.filterManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).filterManual() }))
swiftLauncher.add(name: "ClassArray.countFilteredManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).countFilteredManual() }))
swiftLauncher.add(name: "ClassArray.countFiltered", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).countFiltered() }))
swiftLauncher.add(name: "ClassArray.countFilteredLocal", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { SwiftLauncher.classArrayBenchmark($0).countFilteredLocal() }))

swiftLauncher.add(name: "ClassBaseline.consume", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).consume() }))
swiftLauncher.add(name: "ClassBaseline.consumeField", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).consumeField() }))
swiftLauncher.add(name: "ClassBaseline.allocateList", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).allocateList() }))
swiftLauncher.add(name: "ClassBaseline.allocateArray", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).allocateArray() }))
swiftLauncher.add(name: "ClassBaseline.allocateListAndFill", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).allocateListAndFill() }))
swiftLauncher.add(name: "ClassBaseline.allocateListAndWrite", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).allocateListAndWrite() }))
swiftLauncher.add(name: "ClassBaseline.allocateArrayAndFill", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { SwiftLauncher.classBaselineBenchmark($0).allocateArrayAndFill() }))

swiftLauncher.add(name: "ClassList.filterAndCountWithLambda", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).filterAndCountWithLambda() }))
swiftLauncher.add(name: "ClassList.filterWithLambda", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).filterWithLambda() }))
swiftLauncher.add(name: "ClassList.mapWithLambda", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).mapWithLambda() }))
swiftLauncher.add(name: "ClassList.countWithLambda", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).countWithLambda() }))
swiftLauncher.add(name: "ClassList.filterAndMapWithLambda", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).filterAndMapWithLambda() }))
swiftLauncher.add(name: "ClassList.filterAndMapWithLambdaAsSequence", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).filterAndMapWithLambdaAsSequence() }))
swiftLauncher.add(name: "ClassList.reduce", benchmark: companion.create(ctor: { return ClassListBenchmark() },
        lambda: { SwiftLauncher.classListBenchmark($0).reduce() }))

swiftLauncher.add(name: "ClassStream.copy", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).copy() }))
swiftLauncher.add(name: "ClassStream.copyManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).copyManual() }))
swiftLauncher.add(name: "ClassStream.filterAndCount", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).filterAndCount() }))
swiftLauncher.add(name: "ClassStream.filterAndMap", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).filterAndMap() }))
swiftLauncher.add(name: "ClassStream.filterAndMapManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).filterAndMapManual() }))
swiftLauncher.add(name: "ClassStream.filter", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).filter() }))
swiftLauncher.add(name: "ClassStream.filterManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).filterManual() }))
swiftLauncher.add(name: "ClassStream.countFilteredManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).countFilteredManual() }))
swiftLauncher.add(name: "ClassStream.reduce", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { SwiftLauncher.classStreamBenchmark($0).reduce() }))

swiftLauncher.add(name: "CompanionObject.invokeRegularFunction", benchmark: companion.create(ctor: { return CompanionObjectBenchmark() },
        lambda: { SwiftLauncher.companionObjectBenchmark($0).invokeRegularFunction() }))

swiftLauncher.add(name: "DefaultArgument.testOneOfTwo", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testOneOfTwo() }))
swiftLauncher.add(name: "DefaultArgument.testTwoOfTwo", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testTwoOfTwo() }))
swiftLauncher.add(name: "DefaultArgument.testOneOfFour", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testOneOfFour() }))
swiftLauncher.add(name: "DefaultArgument.testFourOfFour", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testFourOfFour() }))
swiftLauncher.add(name: "DefaultArgument.testOneOfEight", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testOneOfEight() }))
swiftLauncher.add(name: "DefaultArgument.testEightOfEight", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { SwiftLauncher.defaultArgumentBenchmark($0).testEightOfEight() }))

swiftLauncher.add(name: "Elvis.testElvis", benchmark: companion.create(ctor: { return ElvisBenchmark() },
        lambda: { SwiftLauncher.elvisBenchmark($0).testElvis() }))

swiftLauncher.add(name: "Euler.problem1bySequence", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem1bySequence() }))
swiftLauncher.add(name: "Euler.problem1", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem1() }))
swiftLauncher.add(name: "Euler.problem2", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem2() }))
swiftLauncher.add(name: "Euler.problem4", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem4() }))
swiftLauncher.add(name: "Euler.problem8", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem8() }))
swiftLauncher.add(name: "Euler.problem9", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem9() }))
swiftLauncher.add(name: "Euler.problem14", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem14() }))
swiftLauncher.add(name: "Euler.problem14full", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { SwiftLauncher.eulerBenchmark($0).problem14full() }))

swiftLauncher.add(name: "Fibonacci.calcClassic", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { SwiftLauncher.fibonacciBenchmark($0).calcClassic() }))
swiftLauncher.add(name: "Fibonacci.calc", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { SwiftLauncher.fibonacciBenchmark($0).calc() }))
swiftLauncher.add(name: "Fibonacci.calcWithProgression", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { SwiftLauncher.fibonacciBenchmark($0).calcWithProgression() }))
swiftLauncher.add(name: "Fibonacci.calcSquare", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { SwiftLauncher.fibonacciBenchmark($0).calcSquare() }))

swiftLauncher.add(name: "ForLoops.arrayLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).arrayLoop() }))
swiftLauncher.add(name: "ForLoops.floatArrayLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).floatArrayLoop() }))
swiftLauncher.add(name: "ForLoops.charArrayLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).charArrayLoop() }))
swiftLauncher.add(name: "ForLoops.stringLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).stringLoop() }))
swiftLauncher.add(name: "ForLoops.arrayIndicesLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).arrayIndicesLoop() }))
swiftLauncher.add(name: "ForLoops.floatArrayIndicesLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).floatArrayIndicesLoop() }))
swiftLauncher.add(name: "ForLoops.charArrayIndicesLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).charArrayIndicesLoop() }))
swiftLauncher.add(name: "ForLoops.stringIndicesLoop", benchmark: companion.create(ctor: { return ForLoopsBenchmark() },
        lambda: { SwiftLauncher.forLoopsBenchmark($0).stringIndicesLoop() }))

swiftLauncher.add(name: "Inline.calculate", benchmark: companion.create(ctor: { return InlineBenchmark() },
        lambda: { SwiftLauncher.inlineBenchmark($0).calculate() }))
swiftLauncher.add(name: "Inline.calculateInline", benchmark: companion.create(ctor: { return InlineBenchmark() },
        lambda: { SwiftLauncher.inlineBenchmark($0).calculateInline() }))
swiftLauncher.add(name: "Inline.calculateGeneric", benchmark: companion.create(ctor: { return InlineBenchmark() },
        lambda: { SwiftLauncher.inlineBenchmark($0).calculateGeneric() }))
swiftLauncher.add(name: "Inline.calculateGenericInline", benchmark: companion.create(ctor: { return InlineBenchmark() },
        lambda: { SwiftLauncher.inlineBenchmark($0).calculateGenericInline() }))

swiftLauncher.add(name: "IntArray.copy", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).copy() }))
swiftLauncher.add(name: "IntArray.copyManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).copyManual() }))
swiftLauncher.add(name: "IntArray.filterAndCount", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterAndCount() }))
swiftLauncher.add(name: "IntArray.filterSomeAndCount", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterSomeAndCount() }))
swiftLauncher.add(name: "IntArray.filterAndMap", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterAndMap() }))
swiftLauncher.add(name: "IntArray.filterAndMapManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterAndMapManual() }))
swiftLauncher.add(name: "IntArray.filter", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filter() }))
swiftLauncher.add(name: "IntArray.filterSome", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterSome() }))
swiftLauncher.add(name: "IntArray.filterPrime", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterPrime() }))
swiftLauncher.add(name: "IntArray.filterManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterManual() }))
swiftLauncher.add(name: "IntArray.filterSomeManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).filterSomeManual() }))
swiftLauncher.add(name: "IntArray.countFilteredManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredManual() }))
swiftLauncher.add(name: "IntArray.countFilteredSomeManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredSomeManual() }))
swiftLauncher.add(name: "IntArray.countFilteredPrimeManual", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredPrimeManual() }))
swiftLauncher.add(name: "IntArray.countFiltered", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFiltered() }))
swiftLauncher.add(name: "IntArray.countFilteredSome", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredSome() }))
swiftLauncher.add(name: "IntArray.countFilteredPrime", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredPrime() }))
swiftLauncher.add(name: "IntArray.countFilteredLocal", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredLocal() }))
swiftLauncher.add(name: "IntArray.countFilteredSomeLocal", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).countFilteredSomeLocal() }))
swiftLauncher.add(name: "IntArray.reduce", benchmark: companion.create(ctor: { return IntArrayBenchmark() },
        lambda: { SwiftLauncher.intArrayBenchmark($0).reduce() }))

swiftLauncher.add(name: "IntBaseline.consume", benchmark: companion.create(ctor: { return IntBaselineBenchmark() },
        lambda: { SwiftLauncher.intBaselineBenchmark($0).consume() }))
swiftLauncher.add(name: "IntBaseline.allocateArray", benchmark: companion.create(ctor: { return IntBaselineBenchmark() },
        lambda: { SwiftLauncher.intBaselineBenchmark($0).allocateArray() }))
swiftLauncher.add(name: "IntBaseline.allocateArrayAndFill", benchmark: companion.create(ctor: { return IntBaselineBenchmark() },
        lambda: { SwiftLauncher.intBaselineBenchmark($0).allocateArrayAndFill() }))

swiftLauncher.add(name: "IntStream.copy", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).copy() }))
swiftLauncher.add(name: "IntStream.copyManual", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).copyManual() }))
swiftLauncher.add(name: "IntStream.filterAndCount", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).filterAndCount() }))
swiftLauncher.add(name: "IntStream.filterAndMap", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).filterAndMap() }))
swiftLauncher.add(name: "IntStream.filterAndMapManual", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).filterAndMapManual() }))
swiftLauncher.add(name: "IntStream.filter", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).filter() }))
swiftLauncher.add(name: "IntStream.filterManual", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).filterManual() }))
swiftLauncher.add(name: "IntStream.countFilteredManual", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).countFilteredManual() }))
swiftLauncher.add(name: "IntStream.countFiltered", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).countFiltered() }))
swiftLauncher.add(name: "IntStream.countFilteredLocal", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).countFilteredLocal() }))
swiftLauncher.add(name: "IntStream.reduce", benchmark: companion.create(ctor: { return IntStreamBenchmark() },
        lambda: { SwiftLauncher.intStreamBenchmark($0).reduce() }))

swiftLauncher.add(name: "Lambda.noncapturingLambda", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).noncapturingLambda() }))
swiftLauncher.add(name: "Lambda.noncapturingLambdaNoInline", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).noncapturingLambdaNoInline() }))
swiftLauncher.add(name: "Lambda.capturingLambda", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).capturingLambda() }))
swiftLauncher.add(name: "Lambda.capturingLambdaNoInline", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).capturingLambdaNoInline() }))
swiftLauncher.add(name: "Lambda.mutatingLambda", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).mutatingLambda() }))
swiftLauncher.add(name: "Lambda.mutatingLambdaNoInline", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).mutatingLambdaNoInline() }))
swiftLauncher.add(name: "Lambda.methodReference", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).methodReference() }))
swiftLauncher.add(name: "Lambda.methodReferenceNoInline", benchmark: companion.create(ctor: { return LambdaBenchmark() },
        lambda: { SwiftLauncher.lambdaBenchmark($0).methodReferenceNoInline() }))

swiftLauncher.add(name: "Loop.arrayLoop", benchmark: companion.create(ctor: { return LoopBenchmark() },
        lambda: { SwiftLauncher.loopBenchmark($0).arrayLoop() }))
swiftLauncher.add(name: "Loop.arrayIndexLoop", benchmark: companion.create(ctor: { return LoopBenchmark() },
        lambda: { SwiftLauncher.loopBenchmark($0).arrayIndexLoop() }))
swiftLauncher.add(name: "Loop.rangeLoop", benchmark: companion.create(ctor: { return LoopBenchmark() },
        lambda: { SwiftLauncher.loopBenchmark($0).rangeLoop() }))
swiftLauncher.add(name: "Loop.arrayWhileLoop", benchmark: companion.create(ctor: { return LoopBenchmark() },
        lambda: { SwiftLauncher.loopBenchmark($0).arrayWhileLoop() }))
swiftLauncher.add(name: "Loop.arrayForeachLoop", benchmark: companion.create(ctor: { return LoopBenchmark() },
        lambda: { SwiftLauncher.loopBenchmark($0).arrayForeachLoop() }))

swiftLauncher.add(name: "MatrixMap.add", benchmark: companion.create(ctor: { return MatrixMapBenchmark() },
        lambda: { SwiftLauncher.matrixMapBenchmark($0).add() }))

swiftLauncher.add(name: "ParameterNotNull.invokeOneArgWithNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeOneArgWithNullCheck() }))
swiftLauncher.add(name: "ParameterNotNull.invokeOneArgWithoutNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeOneArgWithoutNullCheck() }))
swiftLauncher.add(name: "ParameterNotNull.invokeTwoArgsWithNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeTwoArgsWithNullCheck() }))
swiftLauncher.add(name: "ParameterNotNull.invokeTwoArgsWithoutNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeTwoArgsWithoutNullCheck() }))
swiftLauncher.add(name: "ParameterNotNull.invokeEightArgsWithNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeEightArgsWithNullCheck() }))
swiftLauncher.add(name: "ParameterNotNull.invokeEightArgsWithoutNullCheck", benchmark: companion.create(ctor: { return ParameterNotNullAssertionBenchmark() },
        lambda: { SwiftLauncher.parameterNotNullAssertionBenchmark($0).invokeEightArgsWithoutNullCheck() }))

swiftLauncher.add(name: "PrimeList.calcDirect", benchmark: companion.create(ctor: { return PrimeListBenchmark() },
        lambda: { SwiftLauncher.primeListBenchmark($0).calcDirect() }))
swiftLauncher.add(name: "PrimeList.calcEratosthenes", benchmark: companion.create(ctor: { return PrimeListBenchmark() },
        lambda: { SwiftLauncher.primeListBenchmark($0).calcEratosthenes() }))

swiftLauncher.add(name: "String.stringConcat", benchmark: companion.create(ctor: { return StringBenchmark() },
        lambda: { SwiftLauncher.stringBenchmark($0).stringConcat() }))
swiftLauncher.add(name: "String.stringConcatNullable", benchmark: companion.create(ctor: { return StringBenchmark() },
        lambda: { SwiftLauncher.stringBenchmark($0).stringConcatNullable() }))
swiftLauncher.add(name: "String.stringBuilderConcat", benchmark: companion.create(ctor: { return StringBenchmark() },
        lambda: { SwiftLauncher.stringBenchmark($0).stringBuilderConcat() }))
swiftLauncher.add(name: "String.stringBuilderConcatNullable", benchmark: companion.create(ctor: { return StringBenchmark() },
        lambda: { SwiftLauncher.stringBenchmark($0).stringBuilderConcatNullable() }))
swiftLauncher.add(name: "String.summarizeSplittedCsv", benchmark: companion.create(ctor: { return StringBenchmark() },
        lambda: { SwiftLauncher.stringBenchmark($0).summarizeSplittedCsv() }))

swiftLauncher.add(name: "Switch.testSparseIntSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testSparseIntSwitch() }))
swiftLauncher.add(name: "Switch.testDenseIntSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testDenseIntSwitch() }))
swiftLauncher.add(name: "Switch.testConstSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testConstSwitch() }))
swiftLauncher.add(name: "Switch.testObjConstSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testObjConstSwitch() }))
swiftLauncher.add(name: "Switch.testVarSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testVarSwitch() }))
swiftLauncher.add(name: "Switch.testStringsSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testStringsSwitch() }))
swiftLauncher.add(name: "Switch.testEnumsSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testEnumsSwitch() }))
swiftLauncher.add(name: "Switch.testDenseEnumsSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testDenseEnumsSwitch() }))
swiftLauncher.add(name: "Switch.testSealedWhenSwitch", benchmark: companion.create(ctor: { return SwitchBenchmark() },
        lambda: { SwiftLauncher.switchBenchmark($0).testSealedWhenSwitch() }))

swiftLauncher.add(name: "WithIndicies.withIndicies", benchmark: companion.create(ctor: { return WithIndiciesBenchmark() },
        lambda: { SwiftLauncher.withIndiciesBenchmark($0).withIndicies() }))
swiftLauncher.add(name: "WithIndicies.withIndiciesManual", benchmark: companion.create(ctor: { return WithIndiciesBenchmark() },
        lambda: { SwiftLauncher.withIndiciesBenchmark($0).withIndiciesManual() }))

swiftLauncher.add(name: "OctoTest", benchmark: BenchmarkEntry(
        lambda: { octoTest() }))

swiftLauncher.add(name: "Calls.finalMethod", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).finalMethodCall() }))
swiftLauncher.add(name: "Calls.openMethodMonomorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).classOpenMethodCall_MonomorphicCallsite() }))
swiftLauncher.add(name: "Calls.openMethodBimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).classOpenMethodCall_BimorphicCallsite() }))
swiftLauncher.add(name: "Calls.openMethodTrimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).classOpenMethodCall_TrimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodMonomorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
            lambda: { SwiftLauncher.callsBenchmarks($0).interfaceMethodCall_MonomorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodBimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).interfaceMethodCall_BimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodTrimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).interfaceMethodCall_TrimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodHexamorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).interfaceMethodCall_HexamorphicCallsite() }))
swiftLauncher.add(name: "Calls.returnBoxUnboxFolding", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { SwiftLauncher.callsBenchmarks($0).returnBoxUnboxFolding() }))

swiftLauncher.add(name: "CoordinatesSolver.solve", benchmark: companion.create(ctor: { return CoordinatesSolverBenchmark() },
        lambda: { SwiftLauncher.coordinatesSolverBenchmark($0).solve() }))

swiftLauncher.add(name: "GraphSolver.solve", benchmark: companion.create(ctor: { return GraphSolverBenchmark() },
        lambda: { SwiftLauncher.graphSolverBenchmark($0).solve() }))

swiftLauncher.add(name: "Casts.classCast", benchmark: companion.create(ctor: { return CastsBenchmark() },
        lambda: { SwiftLauncher.castsBenchmark($0).classCast() }))
swiftLauncher.add(name: "Casts.interfaceCast", benchmark: companion.create(ctor: { return CastsBenchmark() },
        lambda: { SwiftLauncher.castsBenchmark($0).interfaceCast() }))

runner.runBenchmarks(args: args, run: { (arguments: BenchmarkArguments) -> [BenchmarkResult] in

    if arguments is BaseBenchmarkArguments {
        let argumentsList: BaseBenchmarkArguments = arguments as! BaseBenchmarkArguments
        return swiftLauncher.launch(numWarmIterations: argumentsList.warmup,
            numberOfAttempts: argumentsList.repeat,
            prefix: argumentsList.prefix, filters: argumentsList.filter,
            filterRegexes: argumentsList.filterRegex,
            verbose: argumentsList.verbose)
    }
    return [BenchmarkResult]()
}, parseArgs: { (args: KotlinArray,  benchmarksListAction: (() -> KotlinUnit)) -> BenchmarkArguments? in
    return runner.parse(args: args, benchmarksListAction: swiftLauncher.benchmarksListAction) },
  collect: { (benchmarks: [BenchmarkResult], arguments: BenchmarkArguments) -> Void in
    runner.collect(results: benchmarks, arguments: arguments)
}, benchmarksListAction: swiftLauncher.benchmarksListAction)