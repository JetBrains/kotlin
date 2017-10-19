package org.jetbrains.kotlin.gradle.plugin

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

    // DSL. Other parameters.

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun languageVersion(version: String)
    fun apiVersion(version: String)

    fun enableDebug(flag: Boolean)
    fun noStdLib(flag: Boolean)
    fun noMain(flag: Boolean)
    fun enableOptimizations(flag: Boolean)
    fun enableAssertions(flag: Boolean)

    fun measureTime(flag: Boolean)
}

interface KonanInteropSpec: KonanBuildingSpec {

    fun defFile(file: Any)

    fun packageName(value: String)

    fun compilerOpts(vararg values: String)

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any)
    fun headers(files: FileCollection)

    fun includeDirs(vararg values: Any)

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun link(vararg files: Any)
    fun link(files: FileCollection)
}