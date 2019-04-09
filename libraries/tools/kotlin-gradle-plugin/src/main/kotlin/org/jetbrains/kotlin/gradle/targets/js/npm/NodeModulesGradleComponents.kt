/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.hash.FileHasher
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.tasks.isKotlinJsRuntimeFile
import org.jetbrains.kotlin.gradle.targets.js.tasks.isZip
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import java.io.File

class NodeModulesGradleComponents(val rootProject: Project) {
    val hasher = (rootProject as ProjectInternal).services.get(FileHasher::class.java)
    val nodeModulesDir = rootProject.rootDir.resolve("node_modules")
    val gson = GsonBuilder().setPrettyPrinting().create()

    private class State {
        val contents: MutableMap<ByteArray, Element> = mutableMapOf()
    }

    private class Element {
        val fileNames: MutableList<String> = mutableListOf()

        @Transient
        val files = mutableListOf<File>()
    }

    class TransitiveNpmDependency(
        val project: String,
        val from: String,
        val key: String,
        val version: String
    )

    private lateinit var old: State
    private val new = State()

    val stateFile = rootProject.buildDir.resolve("nodeModulesGradleComponents.json")

    fun loadOldState() {
        val state: State? = if (stateFile.exists()) try {
            stateFile.reader().use {
                gson.fromJson(it, State::class.java)
            }
        } catch (e: JsonSyntaxException) {
            // todo: warn
            null
        } catch (e: JsonIOException) {
            // todo: warn
            null
        } else null

        old = state ?: State()
    }

    fun sync() {
        val children = nodeModulesDir.list()?.toSet() ?: setOf()
        val deleted = old.contents.keys.toMutableSet()
        val absense = mutableSetOf<ByteArray>()

        new.contents.forEach { (key, value) ->
            deleted.remove(key)
            if (key !in old.contents.keys) absense.add(key)
            else if (value.fileNames.any { it !in children }) absense.add(key)
        }

        deleted.forEach {
            old.contents[it]!!.fileNames.forEach { fileName ->
                nodeModulesDir.resolve(fileName).delete()
            }
        }

        rootProject.copy { copy ->
            absense.forEach { key ->
                copy.from(new.contents[key]!!.files)
                copy.into(nodeModulesDir)
            }
        }

        saveNewState()
    }

    private fun saveNewState() {
        stateFile.writer().use {
            gson.toJson(new, it)
        }
    }

    private fun getOrCompute(file: File, buildElement: (Element) -> Unit) {
        val hash = hasher.hash(file).toByteArray()
        new.contents[hash] = old.contents[hash] ?: Element().also {
            buildElement(it)
            it.files.mapTo(it.fileNames) { it.name }
        }
    }

    fun visitCompilation(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        project: Project,
        transitiveDependencies: MutableList<TransitiveNpmDependency>
    ): List<TransitiveNpmDependency> {
        val kotlin2JsCompile = compilation.compileKotlinTask as Kotlin2JsCompile

        // classpath
        kotlin2JsCompile.classpath.forEach { srcFile ->
            when {
                isKotlinJsRuntimeFile(srcFile) -> getOrCompute(srcFile) { element ->
                    element.files.add(srcFile)
                }
                srcFile.isZip -> getOrCompute(srcFile) { element ->
                    rootProject.zipTree(srcFile).forEach { innerFile ->
                        if (innerFile.name == "package.json") {
                            val packageJson = innerFile.reader().use {
                                gson.fromJson(it, PackageJson::class.java)
                            }

                            packageJson.dependencies.forEach { (key, version) ->
                                transitiveDependencies.add(TransitiveNpmDependency(project.path, srcFile.path, key, version))
                            }
                        } else if (isKotlinJsRuntimeFile(innerFile)) {
                            element.files.add(innerFile)
                        }
                    }
                }
            }
        }

        // output
        kotlin2JsCompile.doLast {
            project.copy { copy ->
                copy.from(kotlin2JsCompile.outputFile)
                copy.from(kotlin2JsCompile.outputFile.path + ".map")
                copy.into(nodeModulesDir)
            }
        }

        return transitiveDependencies
    }
}