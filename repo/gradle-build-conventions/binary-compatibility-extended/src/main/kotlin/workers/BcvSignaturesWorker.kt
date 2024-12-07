/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.workers

import kotlinx.validation.api.*
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.util.jar.JarFile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

abstract class BcvSignaturesWorker : WorkAction<BcvSignaturesWorker.Parameters> {

    private val logger = Logging.getLogger(this::class.java)

    interface Parameters : WorkParameters {
        val outputApiDir: DirectoryProperty

        val inputClasses: ConfigurableFileCollection
        val inputJar: RegularFileProperty

        val publicMarkers: SetProperty<String>
        val publicPackages: SetProperty<String>
        val publicClasses: SetProperty<String>

        val ignoredPackages: SetProperty<String>
        val ignoredMarkers: SetProperty<String>
        val ignoredClasses: SetProperty<String>

        val projectName: Property<String>
    }

    override fun execute() {
        val projectName = parameters.projectName.get()

        val (signaturesCount, duration) = measureTimedValue {
            val signatures = generateSignatures(
                inputJar = parameters.inputJar.orNull,
                inputClasses = parameters.inputClasses,

                publicMarkers = parameters.publicMarkers.get(),
                publicPackages = parameters.publicPackages.get(),
                publicClasses = parameters.publicClasses.get(),

                ignoredPackages = parameters.ignoredPackages.get(),
                ignoredMarkers = parameters.ignoredMarkers.get(),
                ignoredClasses = parameters.ignoredClasses.get(),
            )

            writeSignatures(
                outputApiDir = parameters.outputApiDir.get().asFile,
                projectName = parameters.projectName.get(),
                signatures = signatures
            )

            signatures.count()
        }

        logger.info("BcvSignaturesWorker generated $signaturesCount signatures for $projectName in $duration")
    }

    private fun generateSignatures(
        inputJar: RegularFile?,
        inputClasses: FileCollection,
        publicMarkers: Set<String>,
        publicPackages: Set<String>,
        publicClasses: Set<String>,
        ignoredClasses: Set<String>,
        ignoredMarkers: Set<String>,
        ignoredPackages: Set<String>,
    ): List<ClassBinarySignature> {
        val signatures = when {
            // inputJar takes precedence if specified
            inputJar != null ->
                JarFile(inputJar.asFile).use { it.loadApiFromJvmClasses() }

            !inputClasses.isEmpty -> {
                logger.info("inputClasses: ${inputClasses.files}")

                val filteredInputClasses = inputClasses.asFileTree.matching {
                    exclude("META-INF/**")
                    include("**/*.class")
                }

                logger.info("filteredInputClasses: ${filteredInputClasses.files}")

                filteredInputClasses
                    .asSequence()
                    .map(File::inputStream)
                    .loadApiFromJvmClasses()
            }

            else ->
                error("BcvSignaturesWorker should have either inputClassesDirs, or inputJar property set")
        }

        val publicPackagesNames =
            signatures.extractAnnotatedPackages(publicMarkers.map(::replaceDots).toSet())
        val ignoredPackagesNames =
            signatures.extractAnnotatedPackages(ignoredMarkers.map(::replaceDots).toSet())

        return signatures
            .retainExplicitlyIncludedIfDeclared(
                publicPackages = publicPackages + publicPackagesNames,
                publicClasses = publicClasses,
                publicMarkerAnnotations = publicMarkers,
            )
            .filterOutNonPublic(
                nonPublicPackages = ignoredPackages + ignoredPackagesNames,
                nonPublicClasses = ignoredClasses,
            )
            .filterOutAnnotated(ignoredMarkers.map(::replaceDots).toSet())
    }

    private fun writeSignatures(
        outputApiDir: File,
        projectName: String?,
        signatures: List<ClassBinarySignature>,
    ) {
        outputApiDir.mkdirs()

        outputApiDir
            .resolve("$projectName.api")
            .bufferedWriter().use { writer ->
                signatures.dump(writer)
            }
    }

    companion object {
        // can't use kotlin.time.measureTime {} because Gradle still uses Kotlin LV 1.8
        private fun <T> measureTimedValue(block: () -> T): Pair<T, Duration> {
            val start = System.nanoTime()
            val value = block()
            val end = System.nanoTime()
            return value to (end - start).nanoseconds
        }

        private fun replaceDots(dotSeparated: String): String =
            dotSeparated.replace('.', '/')
    }
}
