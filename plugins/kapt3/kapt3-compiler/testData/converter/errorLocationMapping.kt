// CORRECT_ERROR_TYPES

// EXPECTED_ERROR(10;12) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (33, 5))
// EXPECTED_ERROR(10;19) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 50))
// EXPECTED_ERROR(12;12) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (34, 5))
// EXPECTED_ERROR(12;34) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 62))
// EXPECTED_ERROR(22;18) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 50))
// EXPECTED_ERROR(27;18) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (33, 5))
// EXPECTED_ERROR(28;33) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 62))
// EXPECTED_ERROR(33;5) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (33, 5))
// EXPECTED_ERROR(35;5) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 34))
// EXPECTED_ERROR(36;30) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (26, 34))
// EXPECTED_ERROR(38;18) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (34, 5))
// EXPECTED_ERROR(44;5) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (34, 5))
// EXPECTED_ERROR(54;5) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (37, 5))
// EXPECTED_ERROR(5;11) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (23, 1))
// EXPECTED_ERROR(60;5) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (39, 5))
// EXPECTED_ERROR(73;18) cannot find symbol (Kotlin location: /errorLocationMapping.kt: (42, 5))

@file:Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_PARAMETER_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION")
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