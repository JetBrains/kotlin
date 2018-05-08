/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.junit.Assert

class ScriptingCompilerPluginTest : TestCaseWithTmpdir() {
    fun testScriptResolverEnvironmentArgsParsing() {

        val longStr = (1..100).joinToString { """\" $it aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \\""" }
        val unescapeRe = """\\(["\\])""".toRegex()
        val cmdlineProcessor = ScriptingCommandLineProcessor()
        val configuration = CompilerConfiguration()

        cmdlineProcessor.processOption(
            ScriptingCommandLineProcessor.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION,
            """abc=def,11="ab cd \\ \"",long="$longStr"""",
            configuration
        )

        val res = configuration.getMap(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION)

        Assert.assertEquals(
            hashMapOf("abc" to "def", "11" to "ab cd \\ \"", "long" to unescapeRe.replace(longStr, "\$1")),
            res
        )
    }
}
