package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.FileCollection

interface KonanArtifactSpec {
    fun outputName(name: String)
    fun baseDir(dir: Any)
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
    fun inputDir(dir: Any)

    fun inputFiles(vararg files: Any)
    fun inputFiles(files: Collection<Any>)

    // DSL. Native libraries.

    fun nativeLibrary(lib: Any)
    fun nativeLibraries(vararg libs: Any)
    fun nativeLibraries(libs: FileCollection)

    // DSL. Other parameters.

    fun linkerOpts(args: List<String>)
    fun linkerOpts(vararg args: String)

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

    fun pkg(value: String)

    fun compilerOpts(vararg values: String)

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any)
    fun headers(files: FileCollection)

    fun includeDirs(vararg values: Any)

    fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    fun linkerOpts(values: List<String>)

    fun link(vararg files: Any)
    fun link(files: FileCollection)
}