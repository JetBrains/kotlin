// FIR_DUMP
// FILE: ConstructorExample.kt

import lombok.NoArgsConstructor

@NoArgsConstructor
open class ConstructorExample(var boolean: Boolean, var char: Char, var int: Int, var str: String)

@NoArgsConstructor
class ConstructorExampleWithGenerics<T>(var param: T)

@NoArgsConstructor(force = true)
class ConstructorExampleWithForce(val int: Int) {
    // The following properties should not affect generation
    val x = 5
    val y: String
        get() = "y"
    val z by lazy { "TEST" }
}

// KT-88705: nothing is generated for a value class, whose constructors compile to static `constructor-impl`
// functions - the JVM backend used to fail on the generated constructor's instance initializer. Declaring the
// class is enough to run that codegen.
<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor(force = true)<!>
@JvmInline
value class ConstructorExampleOnValueClass(val value: Int)

fun box(): String {
    val zeroObject = ConstructorExample()
    assertEquals(false, zeroObject.boolean)
    assertEquals(Char(0), zeroObject.char)
    assertEquals(0, zeroObject.int)
    assertEquals(null, zeroObject.str)

    val zeroObjectWithGenerics = ConstructorExampleWithGenerics<Int>()
    assertEquals(null, zeroObjectWithGenerics.param)

    assertEquals(0, ConstructorExampleWithForce().int)

    return "OK"
}
