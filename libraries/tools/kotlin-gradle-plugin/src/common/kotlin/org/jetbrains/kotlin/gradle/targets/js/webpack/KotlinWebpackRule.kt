/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.StringWriter

@Suppress("LeakingThis")
abstract class KotlinWebpackRule {
    @get:Input
    abstract val enabled: Property<Boolean>

    @get:Input
    abstract val test: Property<String>

    @get:Input
    abstract val include: ListProperty<String>

    @get:Input
    abstract val exclude: ListProperty<String>

    @get:Input
    protected open val webpackDescription: String get() = this::class.simpleName ?: "KotlinWebpackRule"

    init {
        enabled.convention(false)
    }

    protected abstract fun buildLoaders(): List<Loader>
    internal abstract fun validate(): Boolean
    internal abstract fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency>

    internal val active: Boolean get() = enabled.getOrElse(false) && validate()
    internal fun Appendable.appendToWebpackConfig() {
        appendLine(
            """
            // $webpackDescription
            ;(function(config) {
            """.trimIndent()
        )
        val loaders = buildLoaders()
        loaders.flatMap(Loader::prerequisites).forEach(::appendLine)
        val use = loaders.joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]",
        ) {
            """
            {
                loader: ${it.value},
                options: ${json(it.options)}
            }
            """.trimIndent()
        }
        appendLine(
            """
            const use = $use
            """.trimIndent()
        )

        val excluded = exclude.get().joinToString(separator = ",", prefix = "[", postfix = "]")
        val included = include.get().joinToString(separator = ",", prefix = "[", postfix = "]")
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

    data class Loader(
        val value: String,
        val options: Map<String, Any?> = mapOf(),
        val prerequisites: List<String> = listOf(),
    )
}
