/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.generators.native.cache.version

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.Path

internal object NativeCacheKotlinVersionsGenerator {
    private val logger = Logger.getLogger(NativeCacheKotlinVersionsGenerator::class.java.name)

    /**
     * Internal model to represent a version and its deprecation status.
     */
    private data class VersionToGenerate(
        val version: Triple<Int, Int, Int>,
        val deprecation: Deprecation,
    ) {
        sealed interface Deprecation {
            object None : Deprecation
            object Warning : Deprecation
            object Error : Deprecation
        }
    }

    /**
     * Applies the business logic for deprecation and filtering.
     * @return A sorted list of versions that should be generated.
     */
    private fun applyDeprecationRules(versions: Set<Triple<Int, Int, Int>>): List<VersionToGenerate> {
        // 1. Partition versions into releases and snapshots (patch == 255)
        val (snapshots, releases) = versions.partition { it.third == 255 }

        // 2. Get the 3 most recent releases for the deprecation cycle
        val sortedReleases = releases.sortedWith(compareBy({ it.first }, { it.second }, { it.third }))
        val releasesToGenerate = sortedReleases.takeLast(3) // Rule: Drop N-3 and older releases

        // 3. Identify N-1, and N-2 *from the release list*
        val deprecated = releasesToGenerate.getOrNull(releasesToGenerate.size - 2) // N-1
        val deprecatedError = releasesToGenerate.getOrNull(releasesToGenerate.size - 3) // N-2

        // 4. Get the 2 most recent snapshots
        val sortedSnapshots = snapshots.sortedWith(compareBy({ it.first }, { it.second }, { it.third }))
        val snapshotsToGenerate = sortedSnapshots.takeLast(2) // Get only two last snapshots

        // 5. Map releases to the intermediate model with deprecation status
        val releaseVersionsToGenerate = releasesToGenerate.map { version ->
            val deprecation = when (version) {
                deprecated -> VersionToGenerate.Deprecation.Warning
                deprecatedError -> VersionToGenerate.Deprecation.Error
                else -> VersionToGenerate.Deprecation.None
            }
            VersionToGenerate(version, deprecation)
        }

        // 6. Map snapshots (they never have deprecation)
        val snapshotVersionsToGenerate = snapshotsToGenerate.map { version ->
            VersionToGenerate(version, VersionToGenerate.Deprecation.None)
        }

        // 7. The final list includes the rolling 4 releases + rolling 2 snapshots
        val allVersionsToGenerate = (releaseVersionsToGenerate + snapshotVersionsToGenerate)
            .sortedWith(compareBy({ it.version.first }, { it.version.second }, { it.version.third }))

        logger.info("Total versions found: ${versions.size}. Found ${releases.size} releases and ${snapshots.size} snapshots. Generating file with ${releasesToGenerate.size} releases (rolling 4) and ${snapshotsToGenerate.size} snapshots (rolling 2).")
        return allVersionsToGenerate
    }

    /**
     * Builds a single `TypeSpec` for a version `object`.
     */
    private fun buildVersionObject(
        version: VersionToGenerate,
        superClass: ClassName
    ): TypeSpec {
        val (major, minor, patch) = version.version
        val propertyName = "${major}_${minor}_${patch}"
        return TypeSpec.objectBuilder(propertyName).apply {
            addModifiers(KModifier.PUBLIC)
            superclass(superClass)
            addSuperclassConstructorParameter("%L, %L, %L", major, minor, patch)
            addKdoc("Represents the Kotlin version constant for %L.%L.%L.", major, minor, patch)

            // Add deprecation annotation if needed
            when (version.deprecation) {
                VersionToGenerate.Deprecation.Warning -> {
                    addAnnotation(
                        AnnotationSpec.builder(Deprecated::class)
                            .addMember(
                                CodeBlock.of(
                                    "message = %S",
                                    "Disabling native cache for this Kotlin version is deprecated. Please re-evaluate if this is still needed. If so, update to the latest version constant. If not, remove this DSL entry."
                                )
                            )
                            .build()
                    )
                }
                VersionToGenerate.Deprecation.Error -> {
                    addAnnotation(
                        AnnotationSpec.builder(Deprecated::class)
                            .addMember(
                                CodeBlock.of(
                                    "message = %S, level = %T.ERROR",
                                    "Disabling native cache for this Kotlin version is no longer supported. Please update to the latest version constant or remove this DSL entry.",
                                    DeprecationLevel::class
                                )
                            )
                            .build()
                    )
                }
                VersionToGenerate.Deprecation.None -> {
                    // No annotation
                }
            }
        }.build()
    }

    /**
     * Builds the final `FileSpec` containing the sealed class.
     */
    private fun buildMainFileSpec(
        className: String,
        superClass: ClassName,
        comparableType: TypeName,
        versionObjects: List<TypeSpec>
    ): FileSpec {
        return FileSpec.builder(KGP_MPP_PACKAGE, className).apply {

            addType(
                TypeSpec.classBuilder(className).apply {
                    addModifiers(KModifier.PUBLIC, KModifier.SEALED)
                    addSuperinterface(comparableType) // Add Comparable interface
                    addKdoc(CLASS_KDOC) // Use the new descriptive KDoc
                    addAnnotation(ANNOTATION_NATIVE_CACHE_API)

                    // Primary constructor: private constructor(public val major/minor/patch)
                    val majorProp = PropertySpec.builder("major", Int::class, KModifier.PUBLIC).initializer("major")
                        .addKdoc("The major version number.").build()
                    val minorProp = PropertySpec.builder("minor", Int::class, KModifier.PUBLIC).initializer("minor")
                        .addKdoc("The minor version number.").build()
                    val patchProp = PropertySpec.builder("patch", Int::class, KModifier.PUBLIC).initializer("patch")
                        .addKdoc("The patch version number.").build()

                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addModifiers(KModifier.PRIVATE)
                            .addParameter("major", Int::class)
                            .addParameter("minor", Int::class)
                            .addParameter("patch", Int::class)
                            .build()
                    )
                    addProperty(majorProp)
                    addProperty(minorProp)
                    addProperty(patchProp)

                    addTypes(versionObjects)
                    addFunction(
                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return \"v\${major}_\${minor}_\${patch}\"")
                            .addKdoc("Returns the string representation of this version (e.g., 'v2_3_0').")
                            .build()
                    )
                    // Add compareTo implementation
                    addFunction(
                        FunSpec.builder("compareTo")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("other", superClass)
                            .returns(Int::class)
                            .addStatement("return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })")
                            .addKdoc("Compares this version to another version.")
                            .build()
                    )
                }.build()
            )
        }.build()
    }

    /**
     * Main entry point for the generator.
     */
    fun generate(versions: Set<Triple<Int, Int, Int>>): Pair<Path, String> {
        val className = "DisableCacheInKotlinVersion"
        val disableCacheInKotlinVersionClass = ClassName(KGP_MPP_PACKAGE, className)
        val comparableType = Comparable::class.asTypeName().parameterizedBy(disableCacheInKotlinVersionClass)

        // 1. Apply all filtering and deprecation rules
        val allVersionsToGenerate = applyDeprecationRules(versions)

        // 2. Build the KotlinPoet TypeSpec for each version object
        val versionObjects = allVersionsToGenerate.map {
            buildVersionObject(it, disableCacheInKotlinVersionClass)
        }

        // 3. Build the final FileSpec
        val mainFile = buildMainFileSpec(
            className = className,
            superClass = disableCacheInKotlinVersionClass,
            comparableType = comparableType,
            versionObjects = versionObjects
        )

        val mainFileAppendable = createGeneratedFileAppendable()
        mainFile.writeTo(mainFileAppendable)
        return Path(mainFile.relativePath) to mainFileAppendable.toString()
    }
}