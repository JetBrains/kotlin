/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.process.CommandLineArgumentProvider

abstract class LazySystemPropertyProvider : CommandLineArgumentProvider {

    @get:Input
    abstract val value: Property<String>

    @get:Input
    abstract val property: Property<String>

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-D${property.get()}=${value.get()}"
        )
    }
}

fun Test.addLazyStringSystemProperty(value: Provider<String>, property: String) {
    val lazyProvider = project.objects.newInstance<LazySystemPropertyProvider>()
    lazyProvider.value.set(value)
    lazyProvider.property.set(property)
    jvmArgumentProviders.add(lazyProvider)
}

fun Test.addLazyBooleanSystemProperty(value: Provider<Boolean>, property: String) {
    addLazyStringSystemProperty(value.map { it.toString() }, property)
}
