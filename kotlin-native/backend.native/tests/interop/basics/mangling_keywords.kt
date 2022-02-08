import kotlin.test.*
import mangling_keywords.*

fun main() {
    // Check that all Kotlin keywords are imported and mangled.
    assertEquals("as", `as`)
    assertEquals("class", `class`)
    assertEquals("dynamic", `dynamic`)
    assertEquals("false", `false`)
    assertEquals("fun", `fun`)
    assertEquals("in", `in`)
    assertEquals("interface", `interface`)
    assertEquals("is", `is`)
    assertEquals("null", `null`)
    assertEquals("object", `object`)
    assertEquals("package", `package`)
    assertEquals("super", `super`)
    assertEquals("this", `this`)
    assertEquals("throw", `throw`)
    assertEquals("true", `true`)
    assertEquals("try", `try`)
    assertEquals("typealias", `typealias`)
    assertEquals("val", `val`)
    assertEquals("var", `var`)
    assertEquals("when", `when`)
}

