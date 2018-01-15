/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// See KT-12694 Java class cannot access (Kotlin) enum with String constructor param

package example

@example.ExampleAnnotation
public class TestClass {
    fun test() {
        val a = 5
        /** insert here */

        println("foo")
    }

    fun check() {
        println("bar")
    }
}