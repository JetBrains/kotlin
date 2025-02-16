@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package test

fun foo() {
    println("some string")
}

@ObjCName("bar")
fun bar() {}