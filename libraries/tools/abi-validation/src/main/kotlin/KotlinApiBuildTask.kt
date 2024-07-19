/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.jar.JarFile
import javax.inject.Inject

private const val MIGRATION_GUIDE_LINK =
    "https://github.com/Kotlin/binary-compatibility-validator/blob/master/docs/design/0.15.0-migration-guide.md"
private const val OUTPUT_API_DIR_ERROR =
    "Property outputApiDir was replaced with outputApiFile. Please refer to the migration guide for migration details: $MIGRATION_GUIDE_LINK"

public abstract class KotlinApiBuildTask @Inject constructor(
) : BuildTaskBase() {
    @get:OutputFile
    public abstract val outputApiFile: RegularFileProperty

    @get:Internal
    @Deprecated(level = DeprecationLevel.ERROR, message = OUTPUT_API_DIR_ERROR)
    public var outputApiDir: File
        get() = throw UnsupportedOperationException(OUTPUT_API_DIR_ERROR)
        set(_) = throw UnsupportedOperationException(OUTPUT_API_DIR_ERROR)

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
        val workQueue = executor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        workQueue.submit(AbiBuildWorker::class.java) { params ->
            fillCommonParams(params)

            params.inputJar.set(inputJar)
            params.inputClassesDirs.from(inputClassesDirs)
            params.outputApiFile.set(outputApiFile)
        }
    }
}

internal interface ApiBuildParameters : BuildParametersBase {
    val outputApiFile: RegularFileProperty
    val inputClassesDirs: ConfigurableFileCollection
    val inputJar: RegularFileProperty
}

internal abstract class AbiBuildWorker : WorkAction<ApiBuildParameters> {
    override fun execute() {
        val inputClassesDirs = parameters.inputClassesDirs
        val signatures = when {
            // inputJar takes precedence if specified
            parameters.inputJar.isPresent ->
                JarFile(parameters.inputJar.get().asFile).use { it.loadApiFromJvmClasses() }

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

        val publicMarkers = parameters.publicMarkers.get()
        val publicClasses = parameters.publicClasses.get()
        val publicPackages = parameters.publicPackages.get()
        val nonPublicMarkers = parameters.nonPublicMarkers.get()
        val ignoredClasses = parameters.ignoredClasses.get()
        val ignoredPackages = parameters.ignoredPackages.get()

        val publicPackagesNames = signatures.extractAnnotatedPackages(publicMarkers.map(::replaceDots).toSet())
        val ignoredPackagesNames =
            signatures.extractAnnotatedPackages(nonPublicMarkers.map(::replaceDots).toSet())

        val filteredSignatures = signatures
            .retainExplicitlyIncludedIfDeclared(
                publicPackages + publicPackagesNames, publicClasses, publicMarkers
            )
            .filterOutNonPublic(ignoredPackages + ignoredPackagesNames, ignoredClasses)
            .filterOutAnnotated(nonPublicMarkers.map(::replaceDots).toSet())

        parameters.outputApiFile.asFile.get().bufferedWriter().use { writer ->
            filteredSignatures.dump(writer)
        }
    }
}
