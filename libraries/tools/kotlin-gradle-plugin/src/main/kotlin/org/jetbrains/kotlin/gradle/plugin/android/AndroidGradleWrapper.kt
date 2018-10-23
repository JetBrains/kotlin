/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object AndroidGradleWrapper {
    fun getRuntimeJars(basePlugin: BasePlugin, baseExtension: BaseExtension): Any? {
        return basePlugin("getRuntimeJarList") ?: baseExtension("getBootClasspath") ?: basePlugin("getBootClasspath")
    }

    fun isJackEnabled(variantData: Any): Boolean {
        val variantConfiguration = variantData("getVariantConfiguration")
        return (variantConfiguration("getUseJack") ?: variantConfiguration("getJackOptions")("isEnabled")) as? Boolean ?: false
    }

    fun getJavaTask(variantData: Any): AbstractCompile? {
        return getJavaCompile(variantData)
    }

    private fun getJavaCompile(baseVariantData: Any): AbstractCompile? {
        return (baseVariantData["javaCompileTask"] ?: baseVariantData["javaCompilerTask"]) as? AbstractCompile
    }

    fun getAnnotationProcessorOptionsFromAndroidVariant(variantData: Any?): Map<String, String>? {
        if (variantData !is BaseVariantData<*>) {
            throw IllegalArgumentException("BaseVariantData instance expected")
        }

        val compileOptions = variantData("getVariantConfiguration")("getJavaCompileOptions") ?: return null

        @Suppress("UNCHECKED_CAST")
        return compileOptions("getAnnotationProcessorOptions")("getArguments") as? Map<String, String>
    }

    fun getVariantDataManager(plugin: BasePlugin): VariantManager {
        return plugin("getVariantManager") as VariantManager
    }


    fun getJavaSources(variantData: BaseVariantData<*>): List<File> {
        val result = LinkedHashSet<File>()

        // user sources
        for (provider in variantData.variantConfiguration.sortedSourceProviders) {
            result.addAll((provider as AndroidSourceSet).java.srcDirs)
        }

        // generated sources
        val javaSources = variantData("getJavaSources")
        if (javaSources is Array<*>) {
            result += javaSources.filterIsInstance<File>()
        } else if (javaSources is List<*>) {
            val fileTrees = javaSources.filterIsInstance<ConfigurableFileTree>()
            result += fileTrees.map { it.dir }
        } else {
            if (variantData.scope.generateRClassTask != null) {
                result += variantData.scope.rClassSourceOutputDir
            }

            if (variantData.scope.generateBuildConfigTask != null) {
                result += variantData.scope.buildConfigSourceOutputDir
            }

            if (variantData.scope.aidlCompileTask != null) {
                result += variantData.scope.aidlSourceOutputDir
            }

            if (variantData.scope.globalScope.extension.dataBinding.isEnabled) {
                result += variantData.scope.classOutputForDataBinding
            }

            if (!variantData.variantConfiguration.renderscriptNdkModeEnabled && variantData.scope.renderscriptCompileTask != null) {
                result += variantData.scope.renderscriptSourceOutputDir
            }
        }

        val extraSources = variantData("getExtraGeneratedSourceFolders") as? List<*>
        if (extraSources != null) {
            result += extraSources.filterIsInstance<File>()
        }

        return result.toList()
    }

    fun getJarToAarMapping(variantData: BaseVariantData<*>): Map<File, File> {
        val jarToLibraryArtifactMap = hashMapOf<File, File>()

        val libraries = getVariantLibraryDependencies(variantData) ?: return jarToLibraryArtifactMap

        for (lib in libraries) {
            lib ?: continue
            val bundle = getLibraryArtifactFile(lib) ?: continue
            val libJarFile = lib("getJarFile") as? File ?: continue

            jarToLibraryArtifactMap[libJarFile] = bundle

            @Suppress("UNCHECKED_CAST")
            val localJarsForLib = lib("getLocalJars") as? List<*> ?: emptyList<Any>()

            // local dependencies are detected as changed by gradle, because they are seem to be
            // rewritten every time when bundle changes
            // when local dep will actually change, record for bundle will be removed from registry
            for (localDep in localJarsForLib) {
                if (localDep is File) {
                    // android tools 2.2
                    jarToLibraryArtifactMap[localDep] = bundle
                } else {
                    // android tools < 2.2
                    val jarFile = localDep("getJarFile") as? File ?: continue
                    jarToLibraryArtifactMap[jarFile] = bundle
                }
            }
        }

        return jarToLibraryArtifactMap
    }

    private fun getLibraryArtifactFile(lib: Any): File? {
        return if (lib.javaClass.name == "com.android.builder.dependency.level2.AndroidDependency") {
            // android tools >= 2.3
            lib("getArtifactFile") as? File
        } else {
            // android tools <= 2.2
            lib("getBundle") as? File
        }
    }

    private fun getVariantLibraryDependencies(variantData: BaseVariantData<*>): Iterable<*>? {
        val variantDependency = variantData.variantDependency

        variantDependency("getAndroidDependencies")?.let {
            // android tools < 2.2
            return it as Iterable<*>
        }

        val compileDependencies = variantDependency("getCompileDependencies") ?: return null
        val result = compileDependencies("getDirectAndroidDependencies") // android >= 2.3
            ?: compileDependencies("getAndroidDependencies") // android 2.2

        return result as? Iterable<*>
    }
}

private operator fun Any?.invoke(methodName: String): Any? {
    if (this == null) {
        return null
    }

    fun Array<Method>.findMethod() =
        singleOrNull { it.name == methodName && it.parameterCount == 0 && !Modifier.isStatic(it.modifiers) }

    val clazz = this::class.java
    val methodToInvoke = clazz.declaredMethods.findMethod() ?: clazz.methods.findMethod() ?: return null
    val oldIsAccessible = methodToInvoke.isAccessible

    try {
        methodToInvoke.isAccessible = true
        return methodToInvoke.invoke(this)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    } catch (e: Throwable) {
        throw RuntimeException("Can't invoke method '$methodName()' on $this", e)
    } finally {
        methodToInvoke.isAccessible = oldIsAccessible
    }
}

private operator fun Any?.get(name: String): Any? {
    if (this == null) {
        return null
    }

    this.invoke("get" + name.capitalize())?.let { return it }

    fun Array<Field>.findField() = singleOrNull { it.name == name && !Modifier.isStatic(it.modifiers) }

    val clazz = this::class.java
    val fieldToInvoke = clazz.declaredFields.findField() ?: clazz.fields.findField() ?: return null
    val oldIsAccessible = fieldToInvoke.isAccessible

    try {
        fieldToInvoke.isAccessible = true
        return fieldToInvoke.get(this)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    } catch (e: Throwable) {
        throw RuntimeException("Couldn't get value of field '$name' in $this", e)
    } finally {
        fieldToInvoke.isAccessible = oldIsAccessible
    }
}