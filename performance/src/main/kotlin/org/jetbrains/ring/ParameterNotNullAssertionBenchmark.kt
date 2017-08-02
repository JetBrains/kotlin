package org.jetbrains.ring

val OBJ = Any()

open class ParameterNotNullAssertionBenchmark {
    
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

    //Benchmark
    fun invokeOneArgWithNullCheck(): Any {
        return methodWithOneNotnullParameter(OBJ)
    }

    //Benchmark
    fun invokeOneArgWithoutNullCheck(): Any {
        return privateMethodWithOneNotnullParameter(OBJ)
    }

    //Benchmark
    fun invokeTwoArgsWithNullCheck(): Any {
        return methodWithTwoNotnullParameters(OBJ, OBJ)
    }

    //Benchmark
    fun invokeTwoArgsWithoutNullCheck(): Any {
        return privateMethodWithTwoNotnullParameters(OBJ, OBJ)
    }

    //Benchmark
    fun invokeEightArgsWithNullCheck(): Any {
        return methodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ)
    }

    //Benchmark
    fun invokeEightArgsWithoutNullCheck(): Any {
        return privateMethodWithEightNotnullParameters(OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ, OBJ)
    }
}

