// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface Semaphore {
    fun release()
}

class ClassA: Semaphore {
    override fun release(): Unit = TODO()
}

class ClassB {
    val hash: Int = 0
    val description: String = ""
    val superclass: KClass<*> get() = TODO()
    fun forwardingTarget(`for`: Any): Unit = TODO()
    fun method(`for`: String): KFunction<Unit> = TODO()
    fun doesNotRecognizeSelector(@ObjCName(swiftName = "_") selector: String): Boolean = TODO()
    @ObjCName(swiftName = "mutableCopyOk")
    fun mutableCopy(): Any = TODO()
    fun hash(@ObjCName(swiftName = "intoOk") into: Any): Unit = TODO()
}

val hash: Int get() = TODO()

fun forwardingTarget(`for`: Any): Unit = TODO()
