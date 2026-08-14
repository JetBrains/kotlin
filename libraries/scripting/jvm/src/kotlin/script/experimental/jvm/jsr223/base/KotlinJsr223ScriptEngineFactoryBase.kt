/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223.base

import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

/**
 * The language-version-agnostic part of a Kotlin JSR-223 [ScriptEngineFactory]. Everything except
 * [getLanguageVersion] and [getEngineVersion] lives here; those are left abstract because reporting
 * the Kotlin compiler version requires the compiler on the classpath.
 */
abstract class KotlinJsr223ScriptEngineFactoryBase : ScriptEngineFactory {

    override fun getLanguageName(): String = "kotlin"
    override fun getEngineName(): String = "kotlin"
    override fun getExtensions(): List<String> = listOf("kts")
    override fun getMimeTypes(): List<String> = listOf("text/x-kotlin")
    override fun getNames(): List<String> = listOf("kotlin")

    override fun getOutputStatement(toDisplay: String?): String = "print(\"$toDisplay\")"
    override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String = "$obj.$m(${args.joinToString()})"

    override fun getProgram(vararg statements: String): String {
        val sep = System.getProperty("line.separator")
        return statements.joinToString(sep) + sep
    }

    override fun getParameter(key: String?): Any? =
        when (key) {
            ScriptEngine.NAME -> engineName
            ScriptEngine.LANGUAGE -> languageName
            ScriptEngine.LANGUAGE_VERSION -> languageVersion
            ScriptEngine.ENGINE -> engineName
            ScriptEngine.ENGINE_VERSION -> engineVersion
            else -> null
        }
}
