/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.daemon.common.toHexString
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.GradleNodeModulesSync.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

/**
 * Incrementally sync gradle JS dependencies into node_modules directory.
 *
 * Working by traversing giving classpath.
 * For each classpath element (called [Element]) `.js` and `.js.map` files are stored into `node_modules`.
 *
 * To do it incrementally, state of previously traversed classpath elements are stored in
 * `node_modules/.from-gradle` file. Unchanged elements will not been visited.
 *
 * See [Element] and [getOrCompute] for more details.
 */
internal class GradleNodeModulesSync(val project: Project) {
    companion object {
        const val STATE_FILE_NAME = ".from-gradle"
    }

    private val hasher = (project as ProjectInternal).services.get(FileHasher::class.java)
    private val npmProjectLayout = NpmProjectLayout[project]
    private val nodeModulesDir = npmProjectLayout.nodeModulesDir
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private class State {
        val contents: MutableMap<String, Element> = mutableMapOf()

        operator fun get(elementHash: ByteArray) =
            contents[elementHash.toHexString()]

        operator fun set(elementHash: ByteArray, element: Element) {
            contents[elementHash.toHexString()] = element
        }
    }

    /**
     * Traversed element from classpath.
     * Stored in state file.
     *
     * When classpath traversed all elements are in one of this states:
     * - unchanged: was loaded from previous state and contents not changed.
     *   only [fileNames] are known and contents of element not visited,
     *   but visitor are saved in [_fillFiles] to load in next case
     * - unchanged but missed in node_modules: some of [fileNames] missed in node_modules
     *   in this case [_fillFiles] called before [files] was getted for copy spec
     * - unchanged and existed in node_modules: nothing will be done in this case
     * - new: not existed in previous state. In this case element is visited,
     *   both [fileNames] and [_files] are filled already
     * - deleted: was loaded from previous state, but not existed in current state
     *   only [fileNames] are known for this case, which is used to remove outdated files
     * - changed: this is just combination of deleted and new state
     */
    private class Element {
        lateinit var source: String
        val fileNames: MutableList<String> = mutableListOf()

        @Transient
        @Suppress("PropertyName")
        var _files: List<File>? = null

        @Transient
        @Suppress("PropertyName")
        var _fillFiles: (() -> List<File>)? = null

        val files: List<File>
            get() {
                if (_files == null) {
                    _files = _fillFiles!!()
                }
                return _files!!
            }
    }

    /**
     * Get info of classpath element from previous state (with storing visitor for case when files are deleted in node_modules)
     * or compute it now.
     *
     * @param file classpath element
     * @param buildElement js files list provider
     */
    private fun getOrCompute(file: File, buildElement: () -> List<File>) {
        val hash = hasher.hash(file).toByteArray()
        val old = old[hash]
        if (old != null) old._fillFiles = buildElement
        new[hash] = old ?: Element().also {
            it.source = file.absolutePath
            it._files = buildElement()
            it._files!!.mapTo(it.fileNames) { it.name }
        }
    }

    private lateinit var old: State
    private val new = State()

    private val stateFile = nodeModulesDir.resolve(STATE_FILE_NAME)

    fun loadOldState() {
        val state: State? = if (stateFile.isFile) try {
            stateFile.reader().use {
                gson.fromJson(it, State::class.java)
            }
        } catch (e: JsonParseException) {
            project.logger.warn("Cannot load previous state of Gradle depencies stored in node_modules", e)
            null
        } else {
            if (stateFile.isDirectory) stateFile.deleteRecursively()
            null
        }

        old = state ?: State()
    }

    fun sync() {
        val children = nodeModulesDir.list()?.toSet() ?: setOf()
        val deleted = old.contents.keys.toMutableSet()
        val absented = mutableSetOf<String>()

        new.contents.forEach { (key, value) ->
            deleted.remove(key)
            if (key !in old.contents.keys) absented.add(key)
            else if (value.fileNames.any { it !in children }) absented.add(key)
        }

        deleted.forEach {
            old.contents[it]!!.fileNames.forEach { fileName ->
                nodeModulesDir.resolve(fileName).delete()
            }
        }

        if (absented.isNotEmpty()) {
            project.copy { copy ->
                absented.forEach { key ->
                    copy.from(new.contents[key]!!.files)
                }
                copy.into(nodeModulesDir)
            }
        }

        if (deleted.isNotEmpty() || absented.isNotEmpty()) {
            saveNewState()
        }
    }

    private fun saveNewState() {
        stateFile.writer().use {
            gson.toJson(new, it)
        }
    }

    class TransitiveNpmDependency(
        val key: String,
        val version: String
    )

    fun visitCompilation(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        project: Project,
        transitiveDependencies: MutableList<TransitiveNpmDependency>
    ): List<TransitiveNpmDependency> {
        val kotlin2JsCompile = compilation.compileKotlinTask as Kotlin2JsCompile

        // classpath
        kotlin2JsCompile.classpath.forEach { srcFile ->
            when {
                srcFile.name == PACKAGE_JSON -> visitPackageJson(srcFile, transitiveDependencies)
                isKotlinJsRuntimeFile(srcFile) -> getOrCompute(srcFile) {
                    listOf(srcFile)
                }
                srcFile.isZip -> getOrCompute(srcFile) {
                    mutableListOf<File>().also { files ->
                        this.project.zipTree(srcFile).forEach { innerFile ->
                            when {
                                innerFile.name == PACKAGE_JSON -> visitPackageJson(innerFile, transitiveDependencies)
                                isKotlinJsRuntimeFile(innerFile) -> files.add(innerFile)
                            }
                        }
                    }
                }
            }
        }

        // output
        if (kotlin2JsCompile.state.executed) {
            copyCompileOutput(project, kotlin2JsCompile)
        } else {
            kotlin2JsCompile.doLast {
                copyCompileOutput(project, kotlin2JsCompile)
            }
        }

        return transitiveDependencies
    }

    private fun copyCompileOutput(project: Project, kotlin2JsCompile: Kotlin2JsCompile) {
        project.copy { copy ->
            copy.from(kotlin2JsCompile.outputFile)
            copy.from(kotlin2JsCompile.outputFile.path + ".map")
            copy.into(nodeModulesDir)
        }
    }

    private fun visitPackageJson(
        innerFile: File,
        transitiveDependencies: MutableList<TransitiveNpmDependency>
    ) {
        val packageJson = innerFile.reader().use {
            gson.fromJson(it, PackageJson::class.java)
        }

        packageJson.dependencies.forEach { (key, version) ->
            transitiveDependencies.add(TransitiveNpmDependency(key, version))
        }
    }

    private val File.isZip
        get() = isFile && (name.endsWith(".jar") || name.endsWith(".zip"))

    private fun isKotlinJsRuntimeFile(file: File): Boolean {
        if (!file.isFile) return false
        val name = file.name
        return (name.endsWith(".js") && !name.endsWith(".meta.js"))
                || name.endsWith(".js.map")
    }
}