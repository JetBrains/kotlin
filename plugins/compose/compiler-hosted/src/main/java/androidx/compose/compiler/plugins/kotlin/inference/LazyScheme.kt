/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

/**
 * A LazyScheme is a scheme that is lazily and progressively inferred by [ApplierInferencer] and is
 * used to store the current inference state for a function type. [Scheme] is used to control the
 * shape and initial values of the lazy scheme. Bound appliers are not changed but open types are
 * inferred or bound together, if possible, by [ApplierInferencer].
 */
class LazyScheme(
    scheme: Scheme,
    context: MutableList<Binding> = mutableListOf(),
    val bindings: Bindings = Bindings(),
) {
    val target: Binding = scheme.target.toBinding(bindings, context)
    val anyParameters = scheme.anyParameters
    val parameters = scheme.parameters.map { LazyScheme(it, context, bindings) }
    val result = scheme.result?.let { LazyScheme(it, context, bindings) }
    val closed: Boolean get() = target.token != null &&
        (result == null || result.closed) && parameters.all { it.closed }

    /**
     * Create a [Scheme] from the current state of this.
     */
    fun toScheme(): Scheme {
        val context: MutableMap<Value, Int> = mutableMapOf()
        var uniqueNumber = 0
        fun mapValues(scheme: LazyScheme) {
            val target = scheme.target
            if (target.token == null) {
                val value = target.value
                val index = context[value]
                if (index == -1) {
                    context[value] = uniqueNumber++
                } else if (index == null) {
                    context[value] = -1
                }
            }
            scheme.parameters.forEach { mapValues(it) }
            scheme.result?.let { mapValues(it) }
        }
        fun itemOf(binding: Binding) = binding.token?.let { Token(it) }
            ?: context[binding.value]?.let { Open(it) } ?: Open(-1)
        fun schemeOf(lazyScheme: LazyScheme): Scheme = Scheme(
            itemOf(lazyScheme.target),
            lazyScheme.parameters.map { schemeOf(it) },
            lazyScheme.result?.let { schemeOf(it) },
            lazyScheme.anyParameters
        )
        mapValues(this)
        return schemeOf(this)
    }

    /**
     * Create a call binding for use when validating a call to the function this lazy scheme is for.
     */
    fun toCallBindings(): CallBindings = CallBindings(
        target,
        parameters.map { it.toCallBindings() },
        result = result?.toCallBindings(),
        anyParameters
    )

    /**
     * Call [callback] whenever the lazy changes.
     */
    fun onChange(callback: () -> Unit): () -> Unit {
        var previousScheme = toScheme()
        return bindings.onChange {
            val newScheme = toScheme()
            if (newScheme != previousScheme) {
                callback()
                previousScheme = newScheme
            }
        }
    }

    override fun toString(): String =
        "[$targetStr$parametersStr$resultStr]"

    private val targetStr get() =
        target.token ?: target.value.index.toString()

    private val parametersStr get() =
        if (parameters.isNotEmpty()) ", ${parameters.joinToString(", ")}"
        else ""

    private val resultStr get() = result?.let { ":$it" } ?: ""

    companion object {
        fun open() = Open(-1).let { target ->
            LazyScheme(
                Scheme(
                    target = target,
                    result = Scheme(target)
                )
            )
        }
    }
}