/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.NODE_MODULES
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules.Companion.JS_SUFFIX
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


/**
 * A custom Gradle task for generating an import map and its corresponding loader script file
 * for JavaScript modules. The task processes dependencies from the `node_modules` directory
 * or a specified input directory.
 *
 * The generated import map is written as a JSON file, and the loader script file is
 * a script that includes the generated import map as an inline "importmap".
 *
 * The task works with Node.js modules and determines their paths based on user-configurable
 * options such as flattened paths and custom path prefixes. It also considers specific
 * module resolution rules in the `node_modules` directory.
 *
 * Task specifics:
 * 1. Generates an import map JSON file listing module paths.
 * 2. Creates a loader script file that injects the import map into an HTML page.
 * 3. Resolves Node.js modules based on standard Node.js resolution rules.
 */
@DisableCachingByDefault
abstract class KotlinImportMapGenerateTask : DefaultTask() {

    /**
     * Represents a collection of artifact files which are outputs of the installation process (e.g. lock files)
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installArtifacts: ConfigurableFileCollection

    /**
     * Represents a directory containing NPM project
     */
    @get:Internal
    abstract val inputDirectory: DirectoryProperty

    /**
     * The path that is used as a base for a relative path of modules
     */
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val nodeModulesDirectory: DirectoryProperty

    /**
     * Represents the output file for the import map generated during the task execution.
     * This file contains the mapping of module names to their respective paths,
     * which is used for resolving module imports for JavaScript projects in JSON format
     */
    @get:OutputFile
    abstract val importMapFile: RegularFileProperty

    /**
     * Represents the output file location for the generated import map loader script.
     * It is supposed to be loaded in index.html
     */
    @get:OutputFile
    abstract val importMapLoaderFile: RegularFileProperty

    /**
     * Indicates whether the generated import map paths should be flattened.
     *
     * When set to `true`, module paths in the import map are flattened to simplify
     * the path structure, typically by removing intermediate directories. If `false`,
     * the paths retain their full directory hierarchy.
     *
     * This property is optional and defaults to `false` if not explicitly set.
     */
    @get:Input
    @get:Optional
    abstract val flattenPaths: Property<Boolean>

    /**
     * Specifies a prefix for module paths in the generated import map.
     *
     * This optional property allows customization of the path prefix used for referencing
     * modules. If specified, the provided prefix will be prepended to the module paths
     * in the resulting import map. This can be useful for aligning module resolution
     * with specific project directory structures or deployment requirements.
     *
     * By default, no prefix is applied.
     */
    @get:Input
    @get:Optional
    abstract val pathPrefix: Property<String>

    /**
     * Indicates whether the build is running on Windows.
     *
     * Windows requires special handling of links in `node_modules`, see [isSymlinkOrJunction].
     */
    @get:Input
    internal abstract val getIsWindows: Property<Boolean>

    @TaskAction
    fun generate() {
        val nodeModulesDir = nodeModulesDirectory.getFile()

        val modules = NpmProjectModules(
            inputDirectory.getFile(),
            packageJsonEntries = listOf("module", "main"),
            indexFileSuffixes = listOf(JS_SUFFIX, ".mjs")
        )

        val importMap = KotlinWebImportMap(
            imports = collectModuleNames(nodeModulesDir.toPath()).associateWith { moduleName ->
                modulePath(modules, moduleName)
            }
        )

        val text = importMap.toJson()
        importMapFile.getFile().writeText(text)

        importMapLoaderFile.getFile().writeText(
            """
            |const script = document.createElement('script');
            |script.type = 'importmap';
            |script.textContent = JSON.stringify($text);
            |document.currentScript.after(script);
            """.trimMargin()
        )
    }

    private fun collectModuleNames(nodeModulesDir: Path): List<String> {
        if (!nodeModulesDir.isDirectory()) return emptyList()

        return nodeModulesDir.listDirectoryEntries()
            // no follow symlinks because in node_modules we have installed workspaces, we don't need to resolve them
            .filter { it.isDirectory() && !it.isSymlinkOrJunction() }
            .flatMap { entry ->
                if (entry.name.startsWith("@")) {
                    if (entry.name.startsWith("@types")) return@flatMap emptyList()

                    entry.listDirectoryEntries()
                        .filter { it.isDirectory() }
                        .map { "${entry.name}/${it.name}" }
                } else {
                    listOf(entry.name)
                }
            }
    }

    /**
     * Checks whether this path is a link that should not be followed: either a symbolic link,
     * or a Windows directory junction.
     *
     * Package managers link workspaces into `node_modules` as symbolic links on Unix-like systems,
     * but as directory junctions on Windows.
     *
     * Junctions use an NTFS reparse tag different from the one used by symbolic links,
     * so [Files.isSymbolicLink] reports `false` for them. Instead, any reparse point is reported
     * as "other" by [BasicFileAttributes.isOther] when links are not followed.
     */
    private fun Path.isSymlinkOrJunction(): Boolean {
        if (isSymbolicLink()) {
            return true
        }

        // Junctions exist on Windows only. On other operating systems "other" files are special files,
        // such as devices, pipes or sockets, which must not be treated as links
        if (!getIsWindows.get()) {
            return false
        }

        val attributes: BasicFileAttributes = try {
            Files.readAttributes(
                this,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS
            )
        } catch (_: IOException) {
            // the entry could be concurrently removed or be inaccessible,
            // in this case there is nothing better to do than to treat it as a usual directory
            return false
        }

        return attributes.isOther
    }

    private fun modulePath(
        modules: NpmProjectModules,
        moduleName: String,
    ): String {
        val resolvedFile = modules.resolve(moduleName) ?: error("Module $moduleName not found")
        val relativeFile = if (flattenPaths.getOrElse(false)) {
            val nodeModulesDir = resolvedFile.nearestNodeModules()
            resolvedFile.relativeTo(nodeModulesDir)
        } else resolvedFile.relativeTo(rootDirectory.getFile())
        return "${pathPrefix.getOrElse("")}/${relativeFile.invariantSeparatorsPath}"
    }

    private fun File.nearestNodeModules(): File {
        var current: File = this
        while (current.name != NODE_MODULES) {
            current = current.parentFile
        }

        return current
    }

    companion object {
        const val NAME = "generateImportMap"
    }
}

/**
 * Represents an [import map](https://html.spec.whatwg.org/multipage/webappapis.html#import-maps)
 * generated by [KotlinImportMapGenerateTask].
 *
 * @property imports mapping of module names to their respective paths
 */
@Serializable
internal data class KotlinWebImportMap(
    val imports: Map<String, String>,
) {
    fun resolveAllModuleDirectories(rootDir: File): Set<File> {
        return imports.values
            .map { path ->
                val relativePath = path.trimStart('/')
                val moduleMainFile = rootDir.resolve(relativePath)
                moduleMainFile.resolveModuleDirectory()
            }
            .toSet()
    }

    /**
     * Resolves the module directory by traversing up from the current file location
     * until a directory containing a "package.json" file is found.
     *
     * @return The parent directory containing the "package.json" file.
     */
    private fun File.resolveModuleDirectory(): File {
        var packageJsonCandidate = resolveSibling(PACKAGE_JSON)
        while (!packageJsonCandidate.exists()) {
            packageJsonCandidate = packageJsonCandidate.parentFile.resolveSibling(PACKAGE_JSON)
        }

        return packageJsonCandidate.parentFile
    }


    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json {
            prettyPrint = true
        }

        fun readFrom(file: File): KotlinWebImportMap = json.decodeFromString(serializer(), file.readText())
    }
}

