/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import java.util.jar.JarFile
import javax.inject.Inject

public abstract class KotlinApiBuildTask @Inject constructor(
) : BuildTaskBase() {
    @get:OutputFile
    public abstract val outputApiFile: RegularFileProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val inputClassesDirs: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val inputJar: RegularFileProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val inputDependencies: ConfigurableFileCollection

    @TaskAction
    internal fun generate() {
        val inputClassesDirs = inputClassesDirs
        val signatures = when {
            // inputJar takes precedence if specified
            inputJar.isPresent ->
                JarFile(inputJar.get().asFile).use { it.loadApiFromJvmClasses() }

            inputClassesDirs.any() ->
                inputClassesDirs.asFileTree.asSequence()
                    .filter {
                        !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                    }
                    .map { it.inputStream() }
                    .loadApiFromJvmClasses()

            else ->
                throw GradleException("KotlinApiBuildTask should have either inputClassesDirs, or inputJar property set")
        }

        val publicPackagesNames = signatures.extractAnnotatedPackages(publicMarkers.get().map(::replaceDots).toSet())
        val ignoredPackagesNames =
            signatures.extractAnnotatedPackages(nonPublicMarkers.get().map(::replaceDots).toSet())

        val filteredSignatures = signatures
            .retainExplicitlyIncludedIfDeclared(
                publicPackages.get() + publicPackagesNames,
                publicClasses.get(), publicMarkers.get()
            )
            .filterOutNonPublic(ignoredPackages.get() + ignoredPackagesNames, ignoredClasses.get())
            .filterOutAnnotated(nonPublicMarkers.get().map(::replaceDots).toSet())

        outputApiFile.asFile.get().bufferedWriter().use { writer ->
            filteredSignatures.dump(writer)
        }
    }
}

