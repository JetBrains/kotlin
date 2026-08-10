/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.providedProperties

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object TestScriptWithReceiversConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(String::class)
    }
)

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithReceiversConfiguration::class)
abstract class TestScriptWithReceivers

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object TestScriptWithSimpleEnvVarsConfiguration : ScriptCompilationConfiguration(
    {
        providedProperties("stringVar1" to String::class)
    }
)

@Suppress("unused")
@KotlinScript(compilationConfiguration = TestScriptWithSimpleEnvVarsConfiguration::class)
abstract class TestScriptWithSimpleEnvVars

@Suppress("unused")
@KotlinScript(fileExtension = "customext")
abstract class TestScriptWithNonKtsExtension(val name: String)

@Suppress("unused")
@KotlinScript(filePathPattern = "(.*/)?pathPattern[0-9]\\..+")
abstract class TestScriptWithPathPattern(val name2: String)

