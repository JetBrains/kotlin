/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.jar.JarFile
import javax.inject.Inject

public open class KotlinApiBuildTask @Inject constructor(
) : DefaultTask() {

    private val extension = project.apiValidationExtensionOrNull

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public var inputClassesDirs: FileCollection? = null

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public val inputJar: RegularFileProperty = this.project.objects.fileProperty()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public lateinit var inputDependencies: FileCollection

    @OutputDirectory
    public lateinit var outputApiDir: File

    private var _ignoredPackages: Set<String>? = null
    @get:Input
    internal var ignoredPackages : Set<String>
        get() = _ignoredPackages ?: extension?.ignoredPackages ?: emptySet()
        set(value) { _ignoredPackages = value }

    private var _nonPublicMarkes: Set<String>? = null
    @get:Input
    internal var nonPublicMarkers : Set<String>
        get() = _nonPublicMarkes ?: extension?.nonPublicMarkers ?: emptySet()
        set(value) { _nonPublicMarkes = value }

    private var _ignoredClasses: Set<String>? = null
    @get:Input
    internal var ignoredClasses : Set<String>
        get() = _ignoredClasses ?: extension?.ignoredClasses ?: emptySet()
        set(value) { _ignoredClasses = value }

    private var _publicPackages: Set<String>? = null
    @get:Input
    internal var publicPackages: Set<String>
        get() = _publicPackages ?: extension?.publicPackages ?: emptySet()
        set(value) { _publicPackages = value }

    private var _publicMarkers: Set<String>? = null
    @get:Input
    internal var publicMarkers: Set<String>
        get() = _publicMarkers ?: extension?.publicMarkers ?: emptySet()
        set(value) { _publicMarkers = value}

    private var _publicClasses: Set<String>? = null
    @get:Input
    internal var publicClasses: Set<String>
        get() = _publicClasses ?: extension?.publicClasses ?: emptySet()
        set(value) { _publicClasses = value }

    @get:Internal
    internal val projectName = project.name

    @TaskAction
    internal fun generate() {
        cleanup(outputApiDir)
        outputApiDir.mkdirs()

        val inputClassesDirs = inputClassesDirs
        val signatures = when {
            // inputJar takes precedence if specified
            inputJar.isPresent ->
                JarFile(inputJar.get().asFile).use { it.loadApiFromJvmClasses() }
            inputClassesDirs != null ->
                inputClassesDirs.asFileTree.asSequence()
                    .filter {
                        !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                    }
                    .map { it.inputStream() }
                    .loadApiFromJvmClasses()
            else ->
                throw GradleException("KotlinApiBuildTask should have either inputClassesDirs, or inputJar property set")
        }

        val publicPackagesNames = signatures.extractAnnotatedPackages(publicMarkers.map(::replaceDots).toSet())
        val ignoredPackagesNames = signatures.extractAnnotatedPackages(nonPublicMarkers.map(::replaceDots).toSet())

        val filteredSignatures = signatures
            .retainExplicitlyIncludedIfDeclared(publicPackages + publicPackagesNames,
                publicClasses, publicMarkers)
            .filterOutNonPublic(ignoredPackages + ignoredPackagesNames, ignoredClasses)
            .filterOutAnnotated(nonPublicMarkers.map(::replaceDots).toSet())

        outputApiDir.resolve("$projectName.api").bufferedWriter().use { writer ->
            filteredSignatures
                .sortedBy { it.name }
                .forEach { api ->
                    writer.append(api.signature).appendLine(" {")
                    api.memberSignatures
                        .sortedWith(MEMBER_SORT_ORDER)
                        .forEach { writer.append("\t").appendLine(it.signature) }
                    writer.appendLine("}\n")
                }
        }
    }

    private fun cleanup(file: File) {
        if (file.exists()) {
            val listing = file.listFiles()
            if (listing != null) {
                for (sub in listing) {
                    cleanup(sub)
                }
            }
            file.delete()
        }
    }
}

