/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import java.io.Serializable
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

// Warning: during the transition to the new REPL infrastructure, should be kept in sync with REPL_CODE_LINE_FIRST_NO/REPL_CODE_LINE_FIRST_GEN
const val REPL_SNIPPET_FIRST_NO = 1
const val REPL_SNIPPET_FIRST_GEN = 1

interface ReplSnippetId : Serializable, Comparable<ReplSnippetId> {
    val no: Int
    val generation: Int
}

data class ReplSnippetIdImpl(override val no: Int, override val generation: Int, private val codeHash: Int) : ReplSnippetId, Serializable {

    constructor(no: Int, generation: Int, code: SourceCode) : this(no, generation, code.text.hashCode())

    override fun compareTo(other: ReplSnippetId): Int = (other as? ReplSnippetIdImpl)?.let { otherId ->
        no.compareTo(otherId.no).takeIf { it != 0 }
            ?: generation.compareTo(otherId.generation).takeIf { it != 0 }
            ?: codeHash.compareTo(otherId.codeHash)
    } ?: -1

    companion object {
        private val serialVersionUID: Long = 1L
    }
}

interface ReplScriptingHostConfigurationKeys

open class ReplScriptingHostConfigurationBuilder : PropertiesCollection.Builder(),
    ReplScriptingHostConfigurationKeys {
    companion object : ReplScriptingHostConfigurationKeys
}

val ScriptingHostConfigurationKeys.repl
    get() = ReplScriptingHostConfigurationBuilder()


interface ReplScriptCompilationConfigurationKeys

open class ReplScriptCompilationConfigurationBuilder : PropertiesCollection.Builder(),
    ReplScriptCompilationConfigurationKeys {
    companion object : ReplScriptCompilationConfigurationKeys
}

val ScriptCompilationConfigurationKeys.repl
    get() = ReplScriptCompilationConfigurationBuilder()


/**
 * The prefix of the name of the generated script class field to assign the snipped results to, empty means disabled
 * see also ScriptCompilationConfigurationKeys.resultField
 */
val ReplScriptCompilationConfigurationKeys.resultFieldPrefix by PropertiesCollection.key<String>("res")

/**
 * The callback that produces synthetic snippets to compile and evaluate before the user snippet.
 * Multiple handlers compose (run in registration order, results concatenated).
 */
val ReplScriptCompilationConfigurationKeys.prependSyntheticSnippets by PropertiesCollection.key<List<PrependSyntheticSnippetsData>>(isTransient = true)


/**
 * Register handler that produces synthetic snippets to be compiled and evaluated before the current snippet
 * @param handler the callback that will be called
 */
fun RefineConfigurationBuilder.prependSyntheticSnippets(handler: PrependSyntheticSnippetsHandler) {
    ScriptCompilationConfiguration.repl.prependSyntheticSnippets.append(PrependSyntheticSnippetsData(handler))
}

/**
 * Handler that returns a synthetic snippet to compile + eval before the current snippet. Non-recursive.
 */
typealias PrependSyntheticSnippetsHandler =
            (ScriptConfigurationRefinementContext) -> ResultWithDiagnostics<Pair<ScriptCompilationConfiguration, SourceCode?>>

data class PrependSyntheticSnippetsData(
    val handler: PrependSyntheticSnippetsHandler
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

fun ScriptCompilationConfiguration.prependSyntheticSnippets(
    script: SourceCode,
    collectedData: ScriptCollectedData? = null
): ResultWithDiagnostics<Pair< ScriptCompilationConfiguration, List<SourceCode>>> {
    val handlers = this[ScriptCompilationConfiguration.repl.prependSyntheticSnippets].orEmpty()
    if (handlers.isEmpty()) return (this to emptyList<SourceCode>()).asSuccess()

    val allSnippets = mutableListOf<SourceCode>()
    var resultingConfiguration = this
    val ctx = ScriptConfigurationRefinementContext(script, this, collectedData)
    for (data in handlers) {
        when (val res = data.handler.invoke(ctx)) {
            is ResultWithDiagnostics.Success -> res.value.let { [cfg, snippet] ->
                resultingConfiguration = cfg
                if (snippet != null) allSnippets.add(snippet)
            }
            is ResultWithDiagnostics.Failure -> return res
        }
    }
    return (resultingConfiguration to allSnippets).asSuccess()
}

typealias MakeSnippetIdentifier = (ScriptCompilationConfiguration, ReplSnippetId) -> String

/**
 * The REPL snippet class identifier generation function
 */
val ReplScriptCompilationConfigurationKeys.makeSnippetIdentifier by PropertiesCollection.key<MakeSnippetIdentifier>(
    { _, snippetId ->
        makeDefaultSnippetIdentifier(snippetId)
    })

fun makeDefaultSnippetIdentifier(snippetId: ReplSnippetId) =
    "Line_${snippetId.no}${if (snippetId.generation > REPL_SNIPPET_FIRST_GEN) "_gen_${snippetId.generation}" else ""}"

/**
 * THIS IS EXTREMELY DANGEROUS, USE WITH CAUTION.
 *
 * A fully qualified name for a function which accepts a parameter-less, trailing lambda.
 * When specified, this function is used to wrap the snippet content during evaluation.
 * This can be used to support `suspend` within a REPL snippet,
 * introduce a new reciever or context parameters,
 * or wrap evaluation with some other custom behavior.
 *
 * For example, given a wrapper named `kotlinx.coroutines.runBlocking', a snippet like:
 * ```kotlin
 * // SNIPPET
 * suspend fun request(url: String) { ... }
 * val response = request("https://...")
 * ```
 *
 * Will be transformed into a snippet like:
 * ```kotlin
 * // SNIPPET
 * kotlinx.coroutines.runBlocking {
 *     suspend fun request(url: String) { ... }
 *     val response = request("https://...")
 * }
 * ```
 *
 * Yet the `response` property will still be accessible from later REPL snippets.
 */
val ReplScriptCompilationConfigurationKeys.internalWrapper by PropertiesCollection.key<String?>()
