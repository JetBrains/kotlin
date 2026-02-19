@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package test

fun foo() {
    println("some longer string")
}

@ObjCName("bar")
fun bar() {}