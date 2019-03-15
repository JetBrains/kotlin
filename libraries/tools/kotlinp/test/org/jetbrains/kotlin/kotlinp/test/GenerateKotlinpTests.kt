/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import org.jetbrains.kotlin.generators.tests.generator.testGroup

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("libraries/tools/kotlinp/test", "libraries/tools/kotlinp/testData") {
        testClass<AbstractKotlinpTest> {
            model("")
        }
    }
}
