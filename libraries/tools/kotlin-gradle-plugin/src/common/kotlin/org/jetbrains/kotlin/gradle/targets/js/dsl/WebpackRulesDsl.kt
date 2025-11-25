/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssRule
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackScssRule
import org.jetbrains.kotlin.gradle.utils.newInstance

/**
 * Common container for the Webpack [rules][KotlinWebpackRule].
 *
 * You can enable the default rules (see [cssSupport] and [scssSupport]),
 * or add custom rules.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
 * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl.commonWebpackConfig
 */
interface WebpackRulesDsl {

    /**
     * The container of all Webpack [rules][KotlinWebpackRule].
     */
    @get:Nested
    val rules: KotlinWebpackRulesContainer

    /**
     * Configures the Webpack [rules].
     */
    fun rules(action: Action<KotlinWebpackRulesContainer>) {
        action.execute(rules)
    }

    /**
     * Configures CSS support for Webpack.
     *
     * By default, CSS support is disabled.
     * To enable it, set [KotlinWebpackCssRule.enabled] to `true`.
     */
    fun cssSupport(action: Action<KotlinWebpackCssRule>) {
        val rule = rules.maybeCreate("css", KotlinWebpackCssRule::class.java)
        action.execute(rule)
    }

    /**
     * Configures SCSS support for Webpack.
     *
     * By default, SCSS support is disabled.
     * To enable it, set [KotlinWebpackScssRule.enabled] to `true`.
     */
    fun scssSupport(action: Action<KotlinWebpackScssRule>) {
        val rule = rules.maybeCreate("scss", KotlinWebpackScssRule::class.java)
        action.execute(rule)
    }

    companion object {

        /** Reified utility function, calls [ExtensiblePolymorphicDomainObjectContainer.registerFactory]. */
        private inline fun <reified T : KotlinWebpackRule> ObjectFactory.bindTo(
            container: ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule>,
        ) = container.registerFactory(T::class.java) { newInstance<T>(it) }

        /**
         * Create a new [KotlinWebpackRulesContainer] instance.
         *
         * **Note:** This interface is not intended for use by build script or plugin authors.
         */
        fun ObjectFactory.webpackRulesContainer(): KotlinWebpackRulesContainer {
            val delegate = polymorphicDomainObjectContainer(KotlinWebpackRule::class.java).also {
                bindTo<KotlinWebpackCssRule>(it)
                bindTo<KotlinWebpackScssRule>(it)
            }
            return KotlinWebpackRulesContainer(delegate, this)
        }
    }
}
