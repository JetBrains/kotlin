// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_ARGUMENT_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION")
import kotlin.reflect.KClass

@Anno(ABC::class)
class ErrorInAnnotation

class ErrorInConstructorParameter(val a: String, val b: ABC, val c: List<ABC>)

class ErrorInSupertype : ABC
class ErrorInSupertype2 : ABC<String>()

class ErrorInDeclarations {
    lateinit var p1: String
    lateinit var p2: ABC
    lateinit var p3: BCD<String>

    fun overloads(a: String) {}
    fun overloads(a: ABC) {}

    fun f1(a: String, b: ABC<List<String>>) {}
    fun <T : String> f2() {}
    fun <T : ABC> f3() {}
    fun f4(): ABC? = null
}

annotation class Anno(val a: KClass<Any>)

// EXPECTED_ERROR(kotlin:11:1) cannot find symbol
// EXPECTED_ERROR(kotlin:12:1) cannot find symbol
// EXPECTED_ERROR(kotlin:16:5) cannot find symbol
// EXPECTED_ERROR(kotlin:17:5) cannot find symbol
// EXPECTED_ERROR(kotlin:20:5) cannot find symbol
// EXPECTED_ERROR(kotlin:22:5) cannot find symbol
// EXPECTED_ERROR(kotlin:25:5) cannot find symbol
// EXPECTED_ERROR(kotlin:6:1) cannot find symbol
// EXPECTED_ERROR(kotlin:9:34) cannot find symbol
// EXPECTED_ERROR(kotlin:9:50) cannot find symbol
// EXPECTED_ERROR(kotlin:9:62) cannot find symbol