/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build.dependeciestxt

import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinModuleKind.COMPILATION_AND_SOURCE_SET_HOLDER
import org.jetbrains.kotlin.config.KotlinModuleKind.SOURCE_SET_HOLDER
import org.jetbrains.kotlin.jps.build.dependeciestxt.ModulesTxt.Dependency.Kind.*
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.impl.FakeK2NativeCompilerArguments
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Modules description file.
 * See [README.md] for more details.
 */
data class ModulesTxt(
    val muted: Boolean,
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

        val dependencies = mutableListOf<Dependency>()
        val usages = mutableListOf<Dependency>()

        val isCommonModule
            get() =
                kotlinFacetSettings?.targetPlatform.isCommon() ||
                        kotlinFacetSettings?.kind == SOURCE_SET_HOLDER

        val isJvmModule
            get() = kotlinFacetSettings?.targetPlatform.isJvm()

        val expectedBy
            get() = dependencies.filter {
                it.kind == EXPECTED_BY ||
                        it.kind == INCLUDE
            }

        @Flag
        var edit: Boolean = false

        @Flag
        var editJvm: Boolean = false

        @Flag
        var editExpectActual: Boolean = false

        lateinit var jpsModule: JpsModule

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
        val kind: Kind,
        val exported: Boolean
    ) {
        val effectivelyExported
            get() = kind == EXPECTED_BY || exported

        init {
            from.dependencies.add(this)
            to.usages.add(this)
        }

        enum class Kind {
            DEPENDENCY,
            EXPECTED_BY,
            INCLUDE
        }
    }
}

class ModulesTxtBuilder {
    var muted = false

    val modules = mutableMapOf<String, ModuleRef>()
    private val dependencies = mutableListOf<DependencyBuilder>()

    /**
     * Reference to module which can be defined later
     */
    class ModuleRef(name: String) {
        var defined: Boolean = false
        var actual: ModulesTxt.Module = ModulesTxt.Module(name)

        override fun toString() = actual.name

        fun build(index: Int): ModulesTxt.Module {
            val result = actual
            result.index = index
            val kotlinFacetSettings = result.kotlinFacetSettings
            if (kotlinFacetSettings != null) {
                kotlinFacetSettings.implementedModuleNames =
                        result.dependencies.asSequence()
                            .filter { it.kind == EXPECTED_BY }
                            .map { it.to.name }
                            .toList()

                kotlinFacetSettings.sourceSetNames =
                        result.dependencies.asSequence()
                            .filter { it.kind == INCLUDE }
                            .map { it.to.name }
                            .toList()
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
        val kind: ModulesTxt.Dependency.Kind,
        val exported: Boolean
    ) {
        fun build(): ModulesTxt.Dependency {
            when (kind) {
                DEPENDENCY -> Unit
                EXPECTED_BY -> check(to.actual.isCommonModule) {
                    "$this: ${to.actual} is not common module"
                }
                INCLUDE -> check(to.actual.kotlinFacetSettings?.kind == SOURCE_SET_HOLDER) {
                    "$this: ${to.actual} is not source set holder"
                }
            }
            return ModulesTxt.Dependency(from.actual, to.actual, scope, kind, exported)
        }
    }

    fun readFile(file: File, fileTitle: String = file.toString()): ModulesTxt {
        try {
            file.forEachLine { line ->
                parseDeclaration(line)
            }

            // dependencies need to be build first: module.build() requires it
            val dependencies = dependencies.map { it.build() }
            val modules = modules.values.mapIndexed { index, moduleRef -> moduleRef.build(index) }

            return ModulesTxt(muted, file, fileTitle, modules, dependencies)
        } catch (t: Throwable) {
            throw Error("Error while reading $file: ${t.message}", t)
        }
    }

    private fun parseDeclaration(line: String) = doParseDeclaration(removeComments(line))

    private fun removeComments(line: String) = line.split("//", limit = 2)[0].trim()

    private fun doParseDeclaration(line: String) {
        when {
            line.isEmpty() -> Unit // skip empty lines
            line == "MUTED" -> {
                muted = true
            }
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

    private fun newModule(def: ValueWithFlags): ModulesTxt.Module {
        val name = def.value.trim()

        val module = ModulesTxt.Module(name)
        val settings = KotlinFacetSettings()
        module.kotlinFacetSettings = settings

        settings.useProjectSettings = false
        settings.compilerSettings = CompilerSettings().also {
            it.additionalArguments = "-version -Xmulti-platform"
        }

        val moduleRef = moduleRef(name)
        check(!moduleRef.defined) { "Module `$name` already defined" }
        moduleRef.defined = true
        moduleRef.actual = module

        def.flags.forEach { flag ->
            when (flag) {
                "sourceSetHolder" -> settings.kind = SOURCE_SET_HOLDER
                "compilationAndSourceSetHolder" -> settings.kind = COMPILATION_AND_SOURCE_SET_HOLDER
                "common" -> settings.compilerArguments =
                    K2MetadataCompilerArguments().also { settings.targetPlatform = CommonPlatforms.defaultCommonPlatform }
                "jvm" -> settings.compilerArguments =
                    K2JVMCompilerArguments().also { settings.targetPlatform = JvmPlatforms.defaultJvmPlatform }
                "js" -> settings.compilerArguments =
                    K2JSCompilerArguments().also { settings.targetPlatform = JsPlatforms.defaultJsPlatform }
                "native" -> settings.compilerArguments =
                    FakeK2NativeCompilerArguments().also { settings.targetPlatform = NativePlatforms.unspecifiedNativePlatform }
                else -> {
                    val flagProperty = ModulesTxt.Module.flags[flag]
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
            var scope: JpsJavaDependencyScope? = null
            var kind: ModulesTxt.Dependency.Kind = DEPENDENCY

            fun setScope(newScope: JpsJavaDependencyScope) {
                check(scope == null) { "`$this: $from -> $to` dependency is already flagged as $scope" }
                scope = newScope
            }

            fun setKind(newKind: ModulesTxt.Dependency.Kind) {
                check(kind == DEPENDENCY) { "`$this: $from -> $to` dependency is already flagged as $kind" }
                kind = newKind
            }

            flags.forEach { flag ->
                when (flag) {
                    "exported" -> exported = true
                    "compile" -> setScope(JpsJavaDependencyScope.COMPILE)
                    "test" -> setScope(JpsJavaDependencyScope.TEST)
                    "runtime" -> setScope(JpsJavaDependencyScope.RUNTIME)
                    "provided" -> setScope(JpsJavaDependencyScope.PROVIDED)
                    "expectedBy" -> setKind(EXPECTED_BY)
                    "include" -> setKind(INCLUDE)
                    else -> error("Unknown dependency flag `$flag`")
                }
            }

            return DependencyBuilder(
                from = moduleRef(from),
                to = moduleRef(to),
                scope = scope ?: JpsJavaDependencyScope.COMPILE,
                kind = kind,
                exported = exported
            ).also {
                dependencies.add(it)
            }
        }
    }
}