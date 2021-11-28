import kotlin.test.*
import mangling_keywords2.*
import kotlinx.cinterop.useContents

fun main() {
    // Check that all Kotlin keywords are imported and mangled.
    createKotlinKeywordsStruct().useContents {
        assertEquals(0, `as`)
        assertEquals(0, `class`)
        assertEquals(0, `dynamic`)
        assertEquals(0, `false`)
        assertEquals(0, `fun`)
        assertEquals(0, `in`)
        assertEquals(0, `interface`)
        assertEquals(0, `is`)
        assertEquals(0, `null`)
        assertEquals(0, `object`)
        assertEquals(0, `package`)
        assertEquals(0, `super`)
        assertEquals(0, `this`)
        assertEquals(0, `throw`)
        assertEquals(0, `true`)
        assertEquals(0, `try`)
        assertEquals(0, `typealias`)
        assertEquals(0, `val`)
        assertEquals(0, `var`)
        assertEquals(0, `when`)
    }

    assertEquals(KotlinKeywordsEnum.`as`, KotlinKeywordsEnum.`as`)
    assertEquals(KotlinKeywordsEnum.`class`, KotlinKeywordsEnum.`class`)
    assertEquals(KotlinKeywordsEnum.`dynamic`, KotlinKeywordsEnum.`dynamic`)
    assertEquals(KotlinKeywordsEnum.`false`, KotlinKeywordsEnum.`false`)
    assertEquals(KotlinKeywordsEnum.`fun`, KotlinKeywordsEnum.`fun`)
    assertEquals(KotlinKeywordsEnum.`in`, KotlinKeywordsEnum.`in`)
    assertEquals(KotlinKeywordsEnum.`interface`, KotlinKeywordsEnum.`interface`)
    assertEquals(KotlinKeywordsEnum.`is`, KotlinKeywordsEnum.`is`)
    assertEquals(KotlinKeywordsEnum.`null`, KotlinKeywordsEnum.`null`)
    assertEquals(KotlinKeywordsEnum.`object`, KotlinKeywordsEnum.`object`)
    assertEquals(KotlinKeywordsEnum.`package`, KotlinKeywordsEnum.`package`)
    assertEquals(KotlinKeywordsEnum.`super`, KotlinKeywordsEnum.`super`)
    assertEquals(KotlinKeywordsEnum.`this`, KotlinKeywordsEnum.`this`)
    assertEquals(KotlinKeywordsEnum.`throw`, KotlinKeywordsEnum.`throw`)
    assertEquals(KotlinKeywordsEnum.`true`, KotlinKeywordsEnum.`true`)
    assertEquals(KotlinKeywordsEnum.`try`, KotlinKeywordsEnum.`try`)
    assertEquals(KotlinKeywordsEnum.`typealias`, KotlinKeywordsEnum.`typealias`)
    assertEquals(KotlinKeywordsEnum.`val`, KotlinKeywordsEnum.`val`)
    assertEquals(KotlinKeywordsEnum.`var`, KotlinKeywordsEnum.`var`)
    assertEquals(KotlinKeywordsEnum.`when`, KotlinKeywordsEnum.`when`)
}