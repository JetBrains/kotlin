/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.gradle.api.Named
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.StringWriter
import javax.inject.Inject

/**
 * A definition of a rule used by Webpack to process files.
 * See https://webpack.js.org/configuration/module/#modulerules
 *
 * KGP will translate [enabled] rules into Webpack configuration.
 *
 * **Note:** This class is not intended for implementation by build script or plugin authors.
 */
@Suppress("LeakingThis")
abstract class KotlinWebpackRule
@Inject
constructor(
    private val name: String,
) : Named {

    /**
     * Controls whether this rule will be added to Webpack.
     */
    @get:Input
    abstract val enabled: Property<Boolean>

    /**
     * See https://webpack.js.org/configuration/module/#ruletest
     */
    @get:Input
    abstract val test: Property<String>

    /**
     * See https://webpack.js.org/configuration/module/#ruleinclude
     */
    @get:Input
    abstract val include: ListProperty<String>

    /**
     * See https://webpack.js.org/configuration/module/#ruleexclude
     */
    @get:Input
    abstract val exclude: ListProperty<String>

    /**
     * A description of this rule.
     *
     * Defaults to the [simple class name][kotlin.reflect.KClass.simpleName] of the rule.
     */
    @get:Input
    protected open val description: String
        get() = (this::class.simpleName?.removeSuffix("_Decorated") ?: "KotlinWebpackRule") + "[${getName()}]"

    init {
        enabled.convention(false)
    }

    /**
     * Validates the rule state just before it getting applied.
     * Returning `false` will skip the rule silently.
     * To terminate the build instead, throw an error.
     */
    open fun validate(): Boolean = true

    /**
     * Provides a list of required npm dependencies for the rule to function.
     */
    open fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency> = listOf()

    /**
     * Provides a loaders sequence to apply to the rule.
     *
     * See https://webpack.js.org/configuration/module/#ruleuse
     */
    protected abstract fun loaders(): List<Loader>

    @get:Internal
    internal val active: Boolean get() = enabled.get() && validate()

    internal fun Appendable.appendToWebpackConfig() {
        appendLine(
            """
            // $description
            ;(function(config) {
            """.trimIndent()
        )
        val loaders = loaders()
        loaders.flatMap(Loader::prerequisites).forEach(::appendLine)
        val use = loaders.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]",
        ) {
            """
            {
                loader: ${it.loader},
                options: ${json(it.options)}
            }
            """.trimIndent()
        }
        appendLine(
            """
            const use = $use
            """.trimIndent()
        )

        val excluded = exclude.get().takeIf(List<*>::isNotEmpty)
            ?.joinToString(separator = ",", prefix = "[", postfix = "]") ?: "undefined"
        val included = include.get().takeIf(List<*>::isNotEmpty)
            ?.joinToString(separator = ",", prefix = "[", postfix = "]") ?: "undefined"
        appendLine(
            """
            config.module.rules.push({
                test: ${test.get()},
                use: use,
                exclude: $excluded,
                include: $included,
            })
            """.trimIndent()
        )
        appendLine(
            """
            })(config);
            
            """.trimIndent()
        )
    }

    protected fun json(obj: Any) = StringWriter().also {
        GsonBuilder().setPrettyPrinting().create().toJson(obj, it)
    }.toString()

    @Internal
    override fun getName(): String = name

    /**
     * Two rules are considered equal if they have the same name.
     * All other fields are ignored.
     */
    override fun equals(other: Any?): Boolean = other is KotlinWebpackRule && getName() == other.getName()

    override fun hashCode(): Int = getName().hashCode()

    /**
     * Define loader configuration.
     *
     * KGP will generate configuration for each loader.
     * Loaders should be
     *
     * See https://webpack.js.org/loaders/
     */
    data class Loader(
        /**
         * Raw `loader` field value. Needs to be wrapped in quotes if using string notation.
         */
        val loader: String,
        /**
         * Loader options map if any. Will be converted to JSON object via Gson.
         */
        val options: Map<String, Any?> = mapOf(),
        /**
         * Any prerequisite code to be added before building the loader object.
         */
        val prerequisites: List<String> = listOf(),
    )
}
