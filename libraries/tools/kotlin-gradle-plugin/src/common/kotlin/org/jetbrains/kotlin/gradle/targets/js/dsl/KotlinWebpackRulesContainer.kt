/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule

/**
 * Container for [KotlinWebpackRule]s.
 *
 * The rules must be configured using [WebpackRulesDsl].
 *
 * **Note:** This class is not intended to be instantiated by build script or plugin authors.
 */
class KotlinWebpackRulesContainer(
    container: ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule>,
    private val objectFactory: ObjectFactory,
) : ExtensiblePolymorphicDomainObjectContainer<KotlinWebpackRule> by container {

    /**
     * Create a new [KotlinWebpackRule] and add it to this container.
     *
     * The class [T] will be instantiated by Gradle.
     * See [Gradle Managed Types](https://docs.gradle.org/9.1.0/userguide/gradle_managed_types_intermediate.html)
     *
     * [name] must be unique.
     */
    inline fun <reified T : KotlinWebpackRule> rule(name: String, config: Action<T> = Action {}) {
        rule(T::class.java, name, config)
    }

    /**
     * Create a new [KotlinWebpackRule] and add it to this container.
     *
     * The class [T] will be instantiated by Gradle.
     * See [Gradle Managed Types](https://docs.gradle.org/9.1.0/userguide/gradle_managed_types_intermediate.html)
     *
     * [name] must be unique.
     */
    fun <T : KotlinWebpackRule> rule(type: Class<T>, name: String, config: Action<T> = Action {}) {
        add(objectFactory.newInstance(type, name).also(config::execute))
    }
}
