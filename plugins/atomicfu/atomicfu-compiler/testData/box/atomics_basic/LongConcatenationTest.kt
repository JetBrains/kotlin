import kotlinx.atomicfu.*
import kotlin.test.*

// See KT-65645
// Recursive transformation of call receivers caused the traverse of the code like this (without any atomics) to hang.
class LongConcatenationTest {
    private val arg1 = "aa"
    private val arg2 = "aa"
    private val arg3 = "aa"
    private val arg4 = "aa"
    private val arg5 = "aa"

    fun loooongConcatenation(): String =
        "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5" +
                "arg1=$arg1" + "arg2=$arg2" + "arg3=$arg3" + "arg4=$arg4" + "arg5=$arg5"
}

fun box(): String {
    val testClass = LongConcatenationTest()
    testClass.loooongConcatenation()
    return "OK"
}
