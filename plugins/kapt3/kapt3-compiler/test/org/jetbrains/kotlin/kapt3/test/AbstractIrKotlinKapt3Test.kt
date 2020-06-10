/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.test.TargetBackend

/*
Currently Kapt3 only works with the old backend. To enable IR, modify the isIrBackend variable computation in GenerationsUtils.compileFiles()
*/

abstract class AbstractIrClassFileToSourceStubConverterTest : AbstractClassFileToSourceStubConverterTest() {
    override val backend = TargetBackend.JVM_IR
}

abstract class AbstractIrKotlinKaptContextTest : AbstractKotlinKaptContextTest() {
    override val backend = TargetBackend.JVM_IR
}
