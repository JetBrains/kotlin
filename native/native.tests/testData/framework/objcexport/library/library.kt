/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package library

interface I {
    val data: String
}

class A(override val data: String): I

enum class E(val data: String) {
    A("Enum entry A"),
    B("Enum entry B"),
    C("Enum entry C")
}

// KT-90589: a cached library with a Kotlin subclass of an Obj-C class must link without `-dead_strip`,
// both for Obj-C methods overridden in Kotlin and for the inherited ones.
// Must be compiled to static cache of a "static framework" and then linked without `-dead_strip`
// to serve as a regression test
class KotlinSubclassOfNSObject : platform.darwin.NSObject() {
    override fun description(): String? = "KotlinSubclassOfNSObject"
}
