/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.embeddable

import org.jetbrains.kotlin.swiftexport.standalone.test.coroutines.main as generateCoroutinesSuite
import org.jetbrains.kotlin.swiftexport.standalone.test.simple.main as generateSimpleSuite

fun main(args: Array<String>) {
    generateSimpleSuite(args)
    generateCoroutinesSuite(args)
}
