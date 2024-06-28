// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: mangling_keywords2.def
---
enum KotlinKeywordsEnum {
    as,
    class,
    dynamic,
    false,
    fun,
    in,
    interface,
    is,
    null,
    object,
    package,
    super,
    this,
    throw,
    true,
    try,
    typealias,
    val,
    var,
    when,
};

struct KotlinKeywordsStruct {
    int as;
    int class;
    int dynamic;
    int false;
    int fun;
    int in;
    int interface;
    int is;
    int null;
    int object;
    int package;
    int super;
    int this;
    int throw;
    int true;
    int try;
        int typealias;
        int val;
        int var;
        int when;
    };

struct KotlinKeywordsStruct createKotlinKeywordsStruct() {
    struct KotlinKeywordsStruct s = {
        .as = 0,
        .class = 0,
        .dynamic = 0,
        .false = 0,
        .fun = 0,
        .in = 0,
        .interface = 0,
        .is = 0,
        .null = 0,
        .object = 0,
        .package = 0,
        .super = 0,
        .this = 0,
        .throw = 0,
        .true = 0,
        .try = 0,
        .typealias = 0,
        .val = 0,
        .var = 0,
        .when = 0,
    };
    return s;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import mangling_keywords2.*
import kotlinx.cinterop.useContents

fun box(): String {
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

    return "OK"
}