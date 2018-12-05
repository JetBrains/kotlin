/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import java.io.File

fun main() {
    generateAbstractKotlinNativeBinaryContainer()
}

internal data class BinaryType(
    val description: String,
    val className: TypeName,
    val nativeOutputKind: TypeName,
    val factoryMethod: String,
    val getMethod: String,
    val findMethod: String
)

private fun binaryType(description: String, className: String, outputKind: String, baseMethodName: String) =
    BinaryType(
        description,
        typeName("$MPP_PACKAGE.$className"),
        typeName("${nativeOutputKindClass.fqName}.$outputKind"),
        baseMethodName,
        "get${baseMethodName.capitalize()}",
        "find${baseMethodName.capitalize()}"
    )

private val nativeBuildTypeClass = typeName("$MPP_PACKAGE.NativeBuildType")
private val nativeOutputKindClass = typeName("$MPP_PACKAGE.NativeOutputKind")
private val nativeBinaryBaseClass = typeName("$MPP_PACKAGE.NativeBinary")

private fun generateFactoryMethods(binaryType: BinaryType): String {
    val className = binaryType.className.renderShort()
    val outputKind = binaryType.nativeOutputKind.renderShort()
    val outputKindClass = nativeOutputKindClass.renderShort()
    val nativeBuildType = nativeBuildTypeClass.renderShort()
    val methodName = binaryType.factoryMethod
    val binaryDescription = binaryType.description

    return """
        /** Creates $binaryDescription with the given [namePrefix] for each build type and configures it. */
        @JvmOverloads
        fun $methodName(
            namePrefix: String,
            buildTypes: Collection<$nativeBuildType> = $nativeBuildType.DEFAULT_BUILD_TYPES,
            configure: $className.() -> Unit = {}
        ) = createBinaries(namePrefix, namePrefix, $outputKindClass.$outputKind, buildTypes, ::$className, configure)

        /** Creates $binaryDescription with the empty name prefix for each build type and configures it. */
        @JvmOverloads
        fun $methodName(
            buildTypes: Collection<$nativeBuildType> = $nativeBuildType.DEFAULT_BUILD_TYPES,
            configure: $className.() -> Unit = {}
        ) = createBinaries("", project.name, $outputKindClass.$outputKind, buildTypes, ::$className, configure)

        /** Creates $binaryDescription with the given [namePrefix] for each build type and configures it. */
        @JvmOverloads
        fun $methodName(
            namePrefix: String,
            buildTypes: Collection<$nativeBuildType> = $nativeBuildType.DEFAULT_BUILD_TYPES,
            configureClosure: Closure<*>
        ) = $methodName(namePrefix, buildTypes) { ConfigureUtil.configure(configureClosure, this) }

        /** Creates $binaryDescription with the default name prefix for each build type and configures it. */
        @JvmOverloads
        fun $methodName(
            buildTypes: Collection<$nativeBuildType> = $nativeBuildType.DEFAULT_BUILD_TYPES,
            configureClosure: Closure<*>
        ) = $methodName(buildTypes) { ConfigureUtil.configure(configureClosure, this) }
    """.trimIndent()
}

private fun generateTypedGetters(binaryType: BinaryType): String = with(binaryType) {
    val className = className.renderShort()
    val buildType = nativeBuildTypeClass.renderShort()
    val nativeBuildType = nativeBuildTypeClass.renderShort()

    return """
        /** Returns $description with the given [namePrefix] and the given build type. Throws an exception if there is no such binary.*/
        abstract fun $getMethod(namePrefix: String, buildType: $buildType): $className

        /** Returns $description with the given [namePrefix] and the given build type. Throws an exception if there is no such binary.*/
        fun $getMethod(namePrefix: String, buildType: String): $className =
            $getMethod(namePrefix, $nativeBuildType.valueOf(buildType.toUpperCase()))

        /** Returns $description with the empty name prefix and the given build type. Throws an exception if there is no such binary.*/
        fun $getMethod(buildType: $buildType): $className = $getMethod("", buildType)

        /** Returns $description with the empty name prefix and the given build type. Throws an exception if there is no such binary.*/
        fun $getMethod(buildType: String): $className =  $getMethod("", buildType)

        /** Returns $description with the given [namePrefix] and the given build type. Returns null if there is no such binary. */
        abstract fun $findMethod(namePrefix: String, buildType: $buildType): $className?

        /** Returns $description with the given [namePrefix] and the given build type. Returns null if there is no such binary. */
        fun $findMethod(namePrefix: String, buildType: String): $className? =
            $findMethod(namePrefix, $nativeBuildType.valueOf(buildType.toUpperCase()))

        /** Returns $description with the empty name prefix and the given build type. Returns null if there is no such binary. */
        fun $findMethod(buildType: $buildType): $className? = $findMethod("", buildType)

        /** Returns $description with the empty name prefix and the given build type. Returns null if there is no such binary. */
        fun $findMethod(buildType: String): $className? = $findMethod("", buildType)
    """.trimIndent()
}

fun generateAbstractKotlinNativeBinaryContainer() {

    val binaryTypes = listOf(
        binaryType("an executable","Executable", "EXECUTABLE", "executable"),
        binaryType("a static library","StaticLibrary", "STATIC", "staticLib"),
        binaryType("a shared library","SharedLibrary", "DYNAMIC", "sharedLib"),
        binaryType("an Objective-C framework","Framework", "FRAMEWORK", "framework")
    )

    val className = typeName("org.jetbrains.kotlin.gradle.dsl.AbstractKotlinNativeBinaryContainer")
    val superClassName = typeName("org.gradle.api.DomainObjectSet", nativeBinaryBaseClass.fqName)

    val imports = """
        import groovy.lang.Closure
        import org.gradle.api.DomainObjectSet
        import org.gradle.api.Project
        import org.gradle.util.ConfigureUtil
        import $MPP_PACKAGE.*
    """.trimIndent()

    val generatedCodeWarning = "// DO NOT EDIT MANUALLY! Generated by ${object {}.javaClass.enclosingClass.name}"

    val classProperties = listOf(
        "abstract val project: Project",
        "abstract val target: ${typeName(KOTLIN_NATIVE_TARGET_CLASS_FQNAME).shortName()}"
    ).joinToString(separator = "\n") { it.indented(4) }

    val nativeBinary = nativeBinaryBaseClass.renderShort()
    val nativeOutputKind = nativeOutputKindClass.renderShort()
    val nativeBuildType = nativeBuildTypeClass.renderShort()

    val buildTypeConstants = listOf(
        "// User-visible constants.",
        "val DEBUG = $nativeBuildType.DEBUG",
        "val RELEASE = $nativeBuildType.RELEASE"
    ).joinToString(separator = "\n") { it.indented(4) }

    val baseFactoryFunction = """
        protected abstract fun <T : $nativeBinary> createBinaries(
            namePrefix: String,
            baseName: String,
            outputKind: $nativeOutputKind,
            buildTypes: Collection<$nativeBuildType>,
            create: (name: String, baseName: String, buildType: $nativeBuildType, compilation: KotlinNativeCompilation) -> T,
            configure: T.() -> Unit
        )
    """.trimIndent().indented(4)

    val namedGetters = """
        /** Provide an access to binaries using the [] operator in Groovy DSL. */
        fun getAt(name: String): NativeBinary = getByName(name)

        /** Provide an access to binaries using the [] operator in Kotlin DSL. */
        operator fun get(name: String): NativeBinary = getByName(name)

        /** Returns a binary with the given [name]. Throws an exception if there is no such binary. */
        abstract fun getByName(name: String): NativeBinary

        /** Returns a binary with the given [name]. Returns null if there is no such binary. */
        abstract fun findByName(name: String): NativeBinary?
    """.trimIndent().indented(4)

    val code = listOf(
        "package ${className.packageName()}",
        imports,
        generatedCodeWarning,
        "abstract class ${className.renderShort()} : ${superClassName.renderShort()} {",
        classProperties,
        buildTypeConstants,
        baseFactoryFunction,
        namedGetters,
        binaryTypes.joinToString(separator = "\n\n") { generateTypedGetters(it).indented(4) },
        binaryTypes.joinToString(separator = "\n\n") { generateFactoryMethods(it).indented(4) },
        "}"
    ).joinToString(separator = "\n\n")

    val outputSourceRoot = System.getProperties()["org.jetbrains.kotlin.generators.gradle.dsl.outputSourceRoot"]
    val targetFile = File("$outputSourceRoot/${className.fqName.replace(".", "/")}.kt")
    targetFile.writeText(code)
}
