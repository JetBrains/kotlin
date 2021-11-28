/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test

import org.apache.logging.log4j.util.Base64Util
import org.apache.logging.log4j.*

@example.ExampleAnnotation
class ModuleUser {

    fun useSomethingFromModule(): String = Base64Util.encode("test")

    fun haveSomeUsageInSignature(): Logger = TODO("I have nothing")
}
