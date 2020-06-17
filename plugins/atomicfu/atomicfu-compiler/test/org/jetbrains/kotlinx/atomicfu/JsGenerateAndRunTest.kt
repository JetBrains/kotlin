/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.BasicIrBoxTest
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlinx.atomicfu.compiler.extensions.AtomicfuComponentRegistrar

private val atomicfuCompileDependency = System.getProperty("atomicfu.classpath")
private val atomicfuRuntime = System.getProperty("atomicfuRuntimeForTests.classpath")

abstract class AtomicfuBaseTest(relativePath: String) : BasicIrBoxTest(
    "plugins/atomicfu/atomicfu-compiler/testData/$relativePath",
    "plugins/atomicfu/atomicfu-compiler/testData/$relativePath",
    listOf(atomicfuCompileDependency, atomicfuRuntime)
) {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return super.createEnvironment().also { environment ->
            AtomicfuComponentRegistrar.registerExtensions(environment.project)
            environment.configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        }
    }
}

abstract class AbstractBasicAtomicfuTest : AtomicfuBaseTest("basic/")
abstract class AbstractLocksAtomicfuTest : AtomicfuBaseTest("locks/")