/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders

import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.refineConfiguration

fun ScriptCompilationConfiguration.Builder.typeProviders(init: TypeProvidersScriptConfigurationBuilder.() -> Unit) {
    val wrappers = TypeProvidersScriptConfigurationBuilder(this).apply(init).build()
    val types = wrappers.map { it.annotationType }

    defaultImports(*types.toTypedArray())
    // TODO: Find a way to get rid of this
    //  needed due to typeOf<T>() usage in generated code when using KType's
    compilerOptions.append("-Xopt-in=kotlin.RequiresOptIn")
    refineConfiguration {
        onAnnotations(types, AnnotationBasedTypeProvidersRefineScriptCompilationConfigurationHandler(wrappers))
    }
}

class TypeProvidersScriptConfigurationBuilder internal constructor(private val builder: ScriptCompilationConfiguration.Builder) {
    private val items = mutableListOf<AnnotationBasedTypeProviderWrapper>()

    fun <A : Annotation> add(provider: AnnotationBasedTypeProvider<A>, annotationType: KClass<A>) {
        with(provider) { builder.prepare() }
        items.add(provider.wrapper(annotationType))
    }

    inline operator fun <reified A : Annotation> AnnotationBasedTypeProvider<A>.unaryPlus() {
        add(this, A::class)
    }

    internal fun build(): List<AnnotationBasedTypeProviderWrapper> {
        return items
    }
}