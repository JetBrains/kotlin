// WITH_STDLIB

// MODULE: lib1
// FILE: f.kt
const val LIB_INT = 1
const val LIB_STR = "LIB_STR"
const val LIB_CHAR = '1'
const val LIB_UINT = 1U
const val LIB_ULINT = 1UL
const val LIB_DOUBLE = 0.1
const val LIB_FLOAT = 0.1f

open class LIB_Class {
    companion object {
        const val STR = "LIB_ClassSTR"
    }
}

// MODULE: main(lib1)
// FILE: main.kt

const val INT = 2
const val STR = "STR"
const val CHAR = '2'
const val UINT = 2U
const val ULINT = 2UL
const val DOUBLE = 0.2
const val FLOAT = 0.2f

open class Class {
    companion object {
        const val STR = "ClassSTR"
    }

    fun testClassCompanion(): String {
        js("""
            function testClassCompanionInnerBox() {            
                if ('$STR' != 'ClassSTR') { return "Not OK Class.STR"; }
                return "OK";
            }
        """)
        return js("testClassCompanionInnerBox()") as String
    }
}

class Child : Class() {
    fun testParentCompanion(): String {
        js("""
            function testParentCompanionInnerBox() {            
                if ('$STR' != 'ClassSTR') { return "Not OK Class.STR"; }
                return "OK";
            }
        """)
        return js("testParentCompanionInnerBox()") as String
    }
}

fun testConstantInlining(): String {
    js("""
        function testConstantInliningInnerBox() {            
            if ($INT != 2) { return "Not OK INT"; }
            if ('$STR' != 'STR') { return "Not OK STR"; }
            if ('$CHAR' != '2') { return "Not OK CHAR"; }
            if ($UINT != '2') { return "Not OK UINT"; }
            if ($ULINT != '2') { return "Not OK ULINT"; }
            if ($DOUBLE > 0.21) { return "Not OK DOUBLE"; }
            if ($FLOAT > 0.21) { return "Not OK FLOAT"; }
            if ('${Class.STR}' != 'ClassSTR') { return "Not OK Class.STR"; }

            return "OK";
        }
    """)
    return js("testConstantInliningInnerBox()") as String
}

fun testConstantInliningFromOtherLib(): String {
    js("""
        function testConstantInliningFromOtherLibInnerBox() {            
            if ($LIB_INT != 1) { return "Not OK LIB_INT"; }
            if ('$LIB_STR' != 'LIB_STR') { return "Not OK LIB_STR"; }
            if ('$LIB_CHAR' != '1') { return "Not OK LIB_CHAR"; }
            if ($LIB_UINT != '1') { return "Not OK LIB_UINT"; }
            if ($LIB_ULINT != '1') { return "Not OK LIB_ULINT"; }
            if ($LIB_DOUBLE > 0.11) { return "Not OK LIB_DOUBLE"; }
            if ($LIB_FLOAT > 0.11) { return "Not OK LIB_FLOAT"; }
            if ('${LIB_Class.STR}' != 'LIB_ClassSTR') { return "Not OK LIB_Class.STR"; }

            return "OK";
        }
    """)
    return js("testConstantInliningFromOtherLibInnerBox()") as String
}

fun testConstEvaluation(): String {
    js("""
        function testConstEvaluationInnerBox() {
            if (${INT != 2}) { return "Not OK INT"; }
            if (${CHAR != '2'}) { return "Not OK CHAR"; }
            if (${STR != "STR"}) { return "Not OK STR"; }
            if (${STR.length} != 3) { return "Not OK STR.length"; }
            if (${Class.STR != "ClassSTR"}) { return "Not OK Class.STR"; }     
            if (${Class.STR.length} != 8) { return "Not OK Class.STR.length"; }
            
            if (${LIB_INT != 1}) { return "Not OK LIB_INT"; }
            if (${LIB_CHAR != '1'}) { return "Not OK LIB_CHAR"; }
            if (${LIB_STR != "LIB_STR"}) { return "Not OK LIB_STR"; }
            if (${LIB_STR.length} != 7) { return "Not OK LIB_STR.length"; }      
            if (${LIB_Class.STR != "LIB_ClassSTR"}) { return "Not OK LIB_Class.STR"; }
            if (${LIB_Class.STR.length} != 12) { return "Not OK LIB_Class.STR.length"; }

            if (${(INT != 2 || CHAR != '2') && (STR == "STR")}) { return "Not OK Boolean operations"; }
                        
            if (${INT + 2 / INT - 3 * INT} != -3) { return "Not OK INT arithmetic operations"; }
            if (${LIB_INT + 2 / LIB_INT - 3 * LIB_INT} != 0) { return "Not OK LIB_INT arithmetic operations"; }
            
            if ('${STR + " " + LIB_Class.STR}' != 'STR LIB_ClassSTR') { return "Not OK STR plus"; }
                        
            return "OK";
        }
    """)
    return js("testConstEvaluationInnerBox()") as String
}

inline fun jsInInlineFunction() {
    js("""
        function jsInInlineFunctionInnerBox() {            
            if ($INT != 2) { return "Not OK INT"; } 
            if ($LIB_INT != 1) { return "Not OK LIB_INT"; }
            if ('${Class.STR}' != 'ClassSTR') { return "Not OK Class.STR"; }
            if ('${LIB_Class.STR}' != 'LIB_ClassSTR') { return "Not OK LIB_Class.STR"; }

            return "OK";
        }
    """)
}

fun testJsInInlineFunction(): String {
    jsInInlineFunction()
    return js("jsInInlineFunctionInnerBox()") as String
}

fun testCaptureVariables(param1: String = "parameter"): String {
    val STR1 = "local variable"

    js("""
        function testCaptureVariablesInnerBox() {            
            if (param$LIB_INT != 'parameter') { return "Not OK parameter"; }
            if ($STR$LIB_INT != 'local variable') { return "Not OK local variable"; }
            
            return "OK";
        }
    """)
    return js("testCaptureVariablesInnerBox()") as String
}

inline fun jsInInlineFunctionCaptureVariables(param1: String = "parameter") {
    val STR1 = "local variable"

    js("""
        function jsInInlineFunctionCaptureVariablesInnerBox() {            
            if (param$LIB_INT != 'parameter') { return "Not OK parameter"; }
            if ($STR$LIB_INT != 'local variable') { return "Not OK local variable"; }
            
            return "OK";
        }
    """)
}

fun testCaptureVariablesInlineFunction(): String {
    jsInInlineFunctionCaptureVariables()
    return js("jsInInlineFunctionCaptureVariablesInnerBox()") as String
}

fun box(): String {
    var r = Class().testClassCompanion()
    if (r != "OK") { return "Fail Class.testClassCompanion: $r" }

    r = Child().testParentCompanion()
    if (r != "OK") { return "Fail Class.testParentCompanion: $r" }

    r = testConstantInlining()
    if (r != "OK") { return "Fail testConstantInlining: $r" }

    r = testConstantInliningFromOtherLib()
    if (r != "OK") { return "Fail testConstantInliningFromOtherLib: $r" }

    r = testConstEvaluation()
    if (r != "OK") { return "Fail testConstEvaluation: $r" }

    r = testJsInInlineFunction()
    if (r != "OK") { return "Fail testJsInInlineFunction: $r" }

    r = testCaptureVariables()
    if (r != "OK") { return "Fail testCaptureVariables: $r" }

    r = testCaptureVariablesInlineFunction()
    if (r != "OK") { return "Fail testCaptureVariablesInlineFunction: $r" }

    return "OK"
}
