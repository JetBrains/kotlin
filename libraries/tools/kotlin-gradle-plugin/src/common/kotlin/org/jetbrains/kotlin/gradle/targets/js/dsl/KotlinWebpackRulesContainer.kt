/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule

class KotlinWebpackRulesContainer(
    container: ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule>,
    private val objectFactory: ObjectFactory,
) : ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule> by container {
    inline fun <reified T : KotlinWebpackRule> rule(name: String, config: Action<T> = Action {}) {
        rule(T::class.java, name, config)
    }

    fun <T : KotlinWebpackRule> rule(type: Class<T>, name: String, config: Action<T> = Action {}) {
        add(objectFactory.newInstance(type, name).also(config::execute))
    }
}
