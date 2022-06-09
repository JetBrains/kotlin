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

interface WebpackRulesDsl {
    @get:Nested
    val rules: KotlinWebpackRulesContainer

    fun rules(action: Action<KotlinWebpackRulesContainer>) {
        action.execute(rules)
    }

    fun cssSupport(action: Action<KotlinWebpackCssRule>) {
        val rule = rules.maybeCreate("css", KotlinWebpackCssRule::class.java)
        action.execute(rule)
    }

    fun scssSupport(action: Action<KotlinWebpackScssRule>) {
        val rule = rules.maybeCreate("scss", KotlinWebpackScssRule::class.java)
        action.execute(rule)
    }

    companion object {
        private inline fun <reified T : KotlinWebpackRule> ObjectFactory.bindTo(
            container: ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule>
        ) = container.registerFactory(T::class.java) { newInstance<T>(it) }

        fun ObjectFactory.webpackRulesContainer(): KotlinWebpackRulesContainer {
            val delegate = polymorphicDomainObjectContainer(KotlinWebpackRule::class.java).also {
                bindTo<KotlinWebpackCssRule>(it)
                bindTo<KotlinWebpackScssRule>(it)
            }
            return KotlinWebpackRulesContainer(delegate, this)
        }
    }
}
