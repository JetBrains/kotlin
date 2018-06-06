/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build.dependeciestxt

import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.TargetPlatformKind
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Dependencies description file.
 * See [README.md] for more details.
 */
data class DependenciesTxt(
    val file: File,
    val fileName: String,
    val modules: List<Module>,
    val dependencies: List<Dependency>
) {
    override fun toString() = fileName

    data class Module(val name: String) {
        var index: Int = -1

        val indexedName
            get() = "${index.toString().padStart(2, '0')}_$name"

        /**
         * Facet should not be created for old tests
         */
        var kotlinFacetSettings: KotlinFacetSettings? = null

        lateinit var jpsModule: JpsModule

        val dependencies = mutableListOf<Dependency>()
        val usages = mutableListOf<Dependency>()

        val isCommonModule
            get() = kotlinFacetSettings?.targetPlatformKind == TargetPlatformKind.Common

        val isJvmModule
            get() = kotlinFacetSettings?.targetPlatformKind is TargetPlatformKind.Jvm

        val expectedBy
            get() = dependencies.filter { it.expectedBy }

        @Flag
        var edit: Boolean = false

        @Flag
        var editJvm: Boolean = false

        @Flag
        var editExpectActual: Boolean = false

        companion object {
            val flags: Map<String, KMutableProperty1<Module, Boolean>> = Module::class.memberProperties
                .filter { it.findAnnotation<Flag>() != null }
                .filterIsInstance<KMutableProperty1<Module, Boolean>>()
                .associateBy { it.name }
        }
    }

    annotation class Flag

    data class Dependency(
        val from: Module,
        val to: Module,
        val scope: JpsJavaDependencyScope,
        val expectedBy: Boolean,
        val exported: Boolean
    ) {
        val effectivelyExported
            get() = expectedBy || exported

        init {
            from.dependencies.add(this)
            to.usages.add(this)
        }
    }
}

class DependenciesTxtBuilder {
    val modules = mutableMapOf<String, ModuleRef>()
    private val dependencies = mutableListOf<DependencyBuilder>()

    /**
     * Reference to module which can be defined later
     */
    class ModuleRef(name: String) {
        var defined: Boolean = false
        var actual: DependenciesTxt.Module = DependenciesTxt.Module(name)

        override fun toString() = actual.name

        fun build(index: Int): DependenciesTxt.Module {
            val result = actual
            result.index = index
            val kotlinFacetSettings = result.kotlinFacetSettings
            if (kotlinFacetSettings != null) {
                kotlinFacetSettings.implementedModuleNames =
                        result.dependencies.filter { it.expectedBy }.map { it.to.name }
            }
            return result
        }
    }

    /**
     * Temporary object for resolving references to modules.
     */
    data class DependencyBuilder(
        val from: ModuleRef,
        val to: ModuleRef,
        val scope: JpsJavaDependencyScope,
        val expectedBy: Boolean,
        val exported: Boolean
    ) {
        fun build(): DependenciesTxt.Dependency {
            if (expectedBy) check(to.actual.isCommonModule) { "$this: ${to.actual} is not common module" }
            return DependenciesTxt.Dependency(from.actual, to.actual, scope, expectedBy, exported)
        }
    }

    fun readFile(file: File, fileTitle: String = file.toString()): DependenciesTxt {
        file.forEachLine { line ->
            parseDeclaration(line)
        }

        // module.build() requires built dependencies
        val dependencies = dependencies.map { it.build() }
        return DependenciesTxt(
            file,
            fileTitle,
            modules.values.mapIndexed { index, moduleRef -> moduleRef.build(index) },
            dependencies
        )
    }

    private fun parseDeclaration(line: String) = doParseDeclaration(removeComments(line))

    private fun removeComments(line: String) = line.split("//", limit = 2)[0].trim()

    private fun doParseDeclaration(line: String) {
        when {
            line.isEmpty() -> Unit // skip empty lines
            line.contains("->") -> {
                val (from, rest) = line.split("->", limit = 2)
                if (rest.isBlank()) {
                    // `name -> ` - module
                    newModule(ValueWithFlags(from))
                } else {
                    val (to, flags) = parseValueWithFlags(rest.trim())
                    newDependency(from.trim(), to.trim(), flags) // `from -> to [flag1, flag2, ...]` - dependency
                }
            }
            else -> newModule(parseValueWithFlags(line)) // `name [flag1, flag2, ...]` - module
        }
    }

    /**
     * `value [flag1, flag2, ...]`
     */
    private fun parseValueWithFlags(str: String): ValueWithFlags {
        val parts = str.split("[", limit = 2)
        return if (parts.size > 1) {
            val (value, flags) = parts
            ValueWithFlags(
                value = value.trim(),
                flags = flags.trim()
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            )
        } else ValueWithFlags(str)
    }

    data class ValueWithFlags(val value: String, val flags: Set<String> = setOf())

    private fun moduleRef(name: String) =
        modules.getOrPut(name) { ModuleRef(name) }

    private fun newModule(def: ValueWithFlags): DependenciesTxt.Module {
        val name = def.value.trim()

        val module = DependenciesTxt.Module(name)
        val kotlinFacetSettings = KotlinFacetSettings()
        module.kotlinFacetSettings = kotlinFacetSettings

        kotlinFacetSettings.useProjectSettings = false
        kotlinFacetSettings.compilerSettings = CompilerSettings().also {
            it.additionalArguments = "-version -Xmulti-platform"
        }

        val moduleRef = moduleRef(name)
        check(!moduleRef.defined) { "Module `$name` already defined" }
        moduleRef.defined = true
        moduleRef.actual = module

        def.flags.forEach { flag ->
            when (flag) {
                "common" -> kotlinFacetSettings.compilerArguments = K2MetadataCompilerArguments()
                "jvm" -> kotlinFacetSettings.compilerArguments = K2JVMCompilerArguments()
                "js" -> kotlinFacetSettings.compilerArguments = K2JSCompilerArguments()
                else -> {
                    val flagProperty = DependenciesTxt.Module.flags[flag]
                    if (flagProperty != null) flagProperty.set(module, true)
                    else error("Unknown module flag `$flag`")
                }
            }
        }

        return module
    }

    private fun newDependency(from: String, to: String, flags: Set<String>): DependencyBuilder? {
        if (to.isEmpty()) {
            // `x -> ` should just create undefined module `x`
            moduleRef(from)

            check(flags.isEmpty()) {
                "`name -> [flag1, flag2, ...]` - not allowed due to the ambiguity of belonging to modules/dependencies. " +
                        "Please use `x [attrs...]` syntax for module attributes."
            }

            return null
        } else {
            var exported = false
            var scope = JpsJavaDependencyScope.COMPILE
            var expectedBy = false

            flags.forEach { flag ->
                when (flag) {
                    "exported" -> exported = true
                    "compile" -> scope = JpsJavaDependencyScope.COMPILE
                    "test" -> scope = JpsJavaDependencyScope.TEST
                    "runtime" -> scope = JpsJavaDependencyScope.RUNTIME
                    "provided" -> scope = JpsJavaDependencyScope.PROVIDED
                    "expectedBy" -> expectedBy = true
                    else -> error("Unknown dependency flag `$flag`")
                }
            }

            return DependencyBuilder(
                from = moduleRef(from),
                to = moduleRef(to),
                scope = scope,
                expectedBy = expectedBy,
                exported = exported
            ).also {
                dependencies.add(it)
            }
        }
    }
}