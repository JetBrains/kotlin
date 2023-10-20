/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface KonanArtifactSpec {
    fun artifactName(name: String)
}

interface KonanArtifactWithLibrariesSpec: KonanArtifactSpec {
    fun libraries(closure: Closure<Unit>)
    fun libraries(action: Action<KonanLibrariesSpec>)
    fun libraries(configure: KonanLibrariesSpec.() -> Unit)

    fun noDefaultLibs(flag: Boolean)
    fun noEndorsedLibs(flag: Boolean)
    fun dependencies(closure: Closure<Unit>)
}

interface KonanBuildingSpec: KonanArtifactWithLibrariesSpec {
    fun dumpParameters(flag: Boolean)

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)
}

interface KonanCompileSpec: KonanBuildingSpec {
    fun srcDir(dir: Any)

    fun srcFiles(vararg files: Any)
    fun srcFiles(files: Collection<Any>)

    // DSL. Native libraries.

    fun nativeLibrary(lib: Any)
    fun nativeLibraries(vararg libs: Any)
    fun nativeLibraries(libs: FileCollection)

    // DSL. Multiplatform projects
    fun enableMultiplatform(flag: Boolean)

    // TODO: Get rid of commonSourceSet in 0.7
    @Deprecated("Use commonSourceSets instead", ReplaceWith("commonSourceSets(sourceSetName)"))
    fun commonSourceSet(sourceSetName: String)
    fun commonSourceSets(vararg sourceSetNames: String)

    fun commonSrcDir(dir: Any)

    fun commonSrcFiles(vararg files: Any)
    fun commonSrcFiles(files: Collection<Any>)

    // DSL. Other parameters.

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun enableDebug(flag: Boolean)
    fun noStdLib(flag: Boolean)
    fun noMain(flag: Boolean)
    fun noPack(flag: Boolean)
    fun enableOptimizations(flag: Boolean)
    fun enableAssertions(flag: Boolean)

    fun entryPoint(entryPoint: String)

    fun measureTime(flag: Boolean)
}

interface KonanInteropSpec: KonanBuildingSpec {

    interface IncludeDirectoriesSpec {
        fun allHeaders(vararg includeDirs: Any)
        fun allHeaders(includeDirs: Collection<Any>)

        fun headerFilterOnly(vararg includeDirs: Any)
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    fun defFile(file: Any)

    fun packageName(value: String)

    fun compilerOpts(vararg values: String)

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any)
    fun headers(files: FileCollection)

    fun includeDirs(vararg values: Any)

    fun includeDirs(closure: Closure<Unit>)
    fun includeDirs(action: Action<IncludeDirectoriesSpec>)
    fun includeDirs(configure: IncludeDirectoriesSpec.() -> Unit)

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun link(vararg files: Any)
    fun link(files: FileCollection)

    fun noPack(flag: Boolean)
}