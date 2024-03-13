/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

interface TestInterface {
    fun doSomething(): Double
}

// These functions have name clashes with an interface, so their names should be mangled with repetitive "_" suffixes
fun TestInterface(double: Double): TestInterface = object : TestInterface {
    override fun doSomething(): Double {
        return double
    }
}
fun TestInterface(int: Int): TestInterface = object : TestInterface {
    override fun doSomething(): Double {
        return int.toDouble()
    }
}

class ContainingClass {
    interface InnerInterface {
        fun doSomething(): Double
    }

    // These functions have name clashes with an interface, so their names should be mangled with repetitive "_" suffixes
    fun InnerInterface(double: Double): InnerInterface = object : InnerInterface {
        override fun doSomething(): Double {
            return double
        }
    }
    fun InnerInterface(int: Int): InnerInterface = object : InnerInterface {
        override fun doSomething(): Double {
            return int.toDouble()
        }
    }
}

// this function is intended to have name clash with package TestPackage
fun TestPackage(double: Double): TestInterface = object : TestInterface {
    override fun doSomething(): Double {
        return double
    }
}