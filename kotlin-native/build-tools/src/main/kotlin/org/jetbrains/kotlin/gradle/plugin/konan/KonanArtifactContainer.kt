/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import kotlin.reflect.KClass

open class KonanArtifactContainer(val project: ProjectInternal) : DefaultPolymorphicDomainObjectContainer<KonanBuildingConfig<*>>(
    KonanBuildingConfig::class.java,
    project.services.get(Instantiator::class.java),
    CollectionCallbackActionDecorator.NOOP
) {
    private inner class KonanBuildingConfigFactory<T: KonanBuildingConfig<*>>(val configClass: KClass<T>)
        : NamedDomainObjectFactory<T> {

        var targets: Iterable<String> = emptyList()

        override fun create(name: String): T =
                instantiator.newInstance(configClass.java, name, project, targets)
    }

    private val factories = mutableMapOf<KClass<out KonanBuildingConfig<*>>, KonanBuildingConfigFactory<*>>()

    private fun <T: KonanBuildingConfig<*>> createFactory(configClass: KClass<T>) {
        val factory = KonanBuildingConfigFactory(configClass)
        super.registerFactory(configClass.java, factory)
        factories.put(configClass, factory)
    }

    init {
        createFactory(KonanProgram::class)
        createFactory(KonanDynamic::class)
        createFactory(KonanFramework::class)
        createFactory(KonanLibrary::class)
        createFactory(KonanBitcode::class)
        createFactory(KonanInteropLibrary::class)
    }

    private fun determineTargets(configClass: KClass<out KonanBuildingConfig<*>>, args: Map<String, Any?>) {
        val targetsArg = args["targets"]
        val targets = when {
            targetsArg == null -> project.konanExtension.targets
            targetsArg is Iterable<*> -> targetsArg.map { it.toString() }
            else -> listOf(targetsArg.toString())
        }
        factories[configClass]?.targets = targets
    }

    private fun <T: KonanBuildingConfig<*>> create(name: String,
                                                   configClass: KClass<T>,
                                                   args: Map<String, Any?>,
                                                   configureAction: Action<T>) {
        determineTargets(configClass, args)
        super.create(name, configClass.java, configureAction)
    }

    private fun <T: KonanBuildingConfig<*>> create(name: String,
                                                   configClass: KClass<T>,
                                                   args: Map<String, Any?>,
                                                   configureAction: T.() -> Unit) {
        determineTargets(configClass, args)
        super.create(name, configClass.java, configureAction)
    }

    private fun <T: KonanBuildingConfig<*>> create(name: String,
                                                   configClass: KClass<T>,
                                                   args: Map<String, Any?>) {
        determineTargets(configClass, args)
        super.create(name, configClass.java)
    }

    fun program(args: Map<String, Any?>, name: String) = create(name, KonanProgram::class, args)
    fun program(args: Map<String, Any?>, name: String, configureAction: Action<KonanProgram>) =
            create(name, KonanProgram::class, args, configureAction)
    fun program(args: Map<String, Any?>, name: String, configureAction: KonanProgram.() -> Unit) =
            create(name, KonanProgram::class, args, configureAction)
    fun program(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            program(args, name) { project.configure(this, configureAction) }

    fun dynamic(args: Map<String, Any?>, name: String) = create(name, KonanDynamic::class, args)
    fun dynamic(args: Map<String, Any?>, name: String, configureAction: Action<KonanDynamic>) =
            create(name, KonanDynamic::class, args, configureAction)
    fun dynamic(args: Map<String, Any?>, name: String, configureAction: KonanDynamic.() -> Unit) =
            create(name, KonanDynamic::class, args, configureAction)
    fun dynamic(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            dynamic(args, name) { project.configure(this, configureAction) }

    fun framework(args: Map<String, Any?>, name: String) = create(name, KonanFramework::class, args)
    fun framework(args: Map<String, Any?>, name: String, configureAction: Action<KonanFramework>) =
            create(name, KonanFramework::class, args, configureAction)
    fun framework(args: Map<String, Any?>, name: String, configureAction: KonanFramework.() -> Unit) =
            create(name, KonanFramework::class, args, configureAction)
    fun framework(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            framework(args, name) { project.configure(this, configureAction) }

    fun library(args: Map<String, Any?>, name: String) = create(name, KonanLibrary::class, args)
    fun library(args: Map<String, Any?>, name: String, configureAction: Action<KonanLibrary>) =
            create(name, KonanLibrary::class, args, configureAction)
    fun library(args: Map<String, Any?>, name: String, configureAction: KonanLibrary.() -> Unit) =
            create(name, KonanLibrary::class, args, configureAction)
    fun library(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            library(args, name) { project.configure(this, configureAction) }

    fun bitcode(args: Map<String, Any?>, name: String) = create(name, KonanBitcode::class, args)
    fun bitcode(args: Map<String, Any?>, name: String, configureAction: Action<KonanBitcode>) =
            create(name, KonanBitcode::class, args, configureAction)
    fun bitcode(args: Map<String, Any?>, name: String, configureAction: KonanBitcode.() -> Unit) =
            create(name, KonanBitcode::class, args, configureAction)
    fun bitcode(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            bitcode(args, name) { project.configure(this, configureAction) }

    fun interop(args: Map<String, Any?>, name: String) = create(name, KonanInteropLibrary::class, args)
    fun interop(args: Map<String, Any?>, name: String, configureAction: Action<KonanInteropLibrary>) =
            create(name, KonanInteropLibrary::class, args, configureAction)
    fun interop(args: Map<String, Any?>, name: String, configureAction: KonanInteropLibrary.() -> Unit) =
            create(name, KonanInteropLibrary::class, args, configureAction)
    fun interop(args: Map<String, Any?>, name: String, configureAction: Closure<*>) =
            interop(args, name) { project.configure(this, configureAction) }

    fun program(name: String)                                           = program(emptyMap(), name)
    fun program(name: String, configureAction: Action<KonanProgram>)    = program(emptyMap(), name, configureAction)
    fun program(name: String, configureAction: KonanProgram.() -> Unit) = program(emptyMap(), name, configureAction)
    fun program(name: String, configureAction: Closure<*>)              = program(emptyMap(), name, configureAction)

    fun dynamic(name: String)                                           = dynamic(emptyMap(), name)
    fun dynamic(name: String, configureAction: Action<KonanDynamic>)    = dynamic(emptyMap(), name, configureAction)
    fun dynamic(name: String, configureAction: KonanDynamic.() -> Unit) = dynamic(emptyMap(), name, configureAction)
    fun dynamic(name: String, configureAction: Closure<*>)              = dynamic(emptyMap(), name, configureAction)

    fun framework(name: String)                                             = framework(emptyMap(), name)
    fun framework(name: String, configureAction: Action<KonanFramework>)    = framework(emptyMap(), name, configureAction)
    fun framework(name: String, configureAction: KonanFramework.() -> Unit) = framework(emptyMap(), name, configureAction)
    fun framework(name: String, configureAction: Closure<*>)                = framework(emptyMap(), name, configureAction)

    fun library(name: String)                                           = library(emptyMap(), name)
    fun library(name: String, configureAction: Action<KonanLibrary>)    = library(emptyMap(), name, configureAction)
    fun library(name: String, configureAction: KonanLibrary.() -> Unit) = library(emptyMap(), name, configureAction)
    fun library(name: String, configureAction: Closure<*>)              = library(emptyMap(), name, configureAction)

    fun bitcode(name: String)                                           = bitcode(emptyMap(), name)
    fun bitcode(name: String, configureAction: Action<KonanBitcode>)    = bitcode(emptyMap(), name, configureAction)
    fun bitcode(name: String, configureAction: KonanBitcode.() -> Unit) = bitcode(emptyMap(), name, configureAction)
    fun bitcode(name: String, configureAction: Closure<*>)              = bitcode(emptyMap(), name, configureAction)

    fun interop(name: String)                                                  = interop(emptyMap(), name)
    fun interop(name: String, configureAction: Action<KonanInteropLibrary>)    = interop(emptyMap(), name, configureAction)
    fun interop(name: String, configureAction: KonanInteropLibrary.() -> Unit) = interop(emptyMap(), name, configureAction)
    fun interop(name: String, configureAction: Closure<*>)                     = interop(emptyMap(), name, configureAction)

}
