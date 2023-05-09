package org.jetbrains.kotlin.script.jsr223

import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import javax.script.Bindings
import javax.script.ScriptEngine
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.templates.standard.ScriptTemplateWithBindings

@Suppress("unused")
@ScriptTemplateDefinition
@Deprecated("Use kotlin-scripting-jsr223 instead")
abstract class KotlinStandardJsr223ScriptTemplate(val jsr223Bindings: Bindings) : ScriptTemplateWithBindings(jsr223Bindings) {

    private val myEngine: ScriptEngine? get() = bindings[KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY]?.let { it as? ScriptEngine }

    private inline fun<T> withMyEngine(body: (ScriptEngine) -> T): T =
            myEngine?.let(body) ?: throw IllegalStateException("Script engine for `eval` call is not found")

    fun eval(script: String, newBindings: Bindings): Any? =
            withMyEngine {
                val savedState = newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY]?.takeIf { it === this.jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] }?.apply {
                    newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = null
                }
                val res = it.eval(script, newBindings)
                savedState?.apply {
                    newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
                }
                res
            }

    fun eval(script: String): Any? =
            withMyEngine {
                val savedState = jsr223Bindings.remove(KOTLIN_SCRIPT_STATE_BINDINGS_KEY)
                val res = it.eval(script, jsr223Bindings)
                savedState?.apply {
                    jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
                }
                res
            }

    fun createBindings(): Bindings = withMyEngine { it.createBindings() }
}