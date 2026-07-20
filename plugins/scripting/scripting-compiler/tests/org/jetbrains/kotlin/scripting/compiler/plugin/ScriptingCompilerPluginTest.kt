/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ScriptingCompilerPluginTest {
    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testScriptResolverEnvironmentArgsParsing() {

        val longStr = (1..100).joinToString("\\,") { """\" $it aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \\""" }
        val unescapeRe = """\\(["\\,])""".toRegex()
        val cmdlineProcessor = ScriptingCommandLineProcessor()
        val configuration = CompilerConfiguration.create()

        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION as AbstractCliOption,
            """abc=def,11="ab cd \\ \"",long="$longStr"""",
            configuration
        )

        val res = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)

        assertEquals(
            hashMapOf("abc" to "def", "11" to "ab cd \\ \"", "long" to unescapeRe.replace(longStr, "\$1")),
            res
        )
    }

}

fun MessageCollectorImpl.assertHasMessage(msg: String, desiredSeverity: CompilerMessageSeverity? = null) {
    assert(messages.any { it.message.contains(msg) && (desiredSeverity == null || it.severity == desiredSeverity) }) {
        "Expecting message \"$msg\" with severity ${desiredSeverity?.toString() ?: "Any"}, actual:\n" +
                messages.joinToString("\n") { it.severity.toString() + ": " + it.message }
    }
}

fun assertTrue(exp: Boolean, msg: () -> String) {
    if (!exp) {
        fail(msg())
    }
}
