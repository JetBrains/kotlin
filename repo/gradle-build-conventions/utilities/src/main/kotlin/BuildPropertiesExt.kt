/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val KotlinBuildProperties.includeJava9: Boolean
    get() = booleanProperty("kotlin.build.java9", true).get()

val KotlinBuildProperties.useBootstrapStdlib: Boolean
    get() = booleanProperty("kotlin.build.useBootstrapStdlib", false).get()

val KotlinBuildProperties.postProcessing: Boolean get() = isTeamcityBuild.get() || booleanProperty("kotlin.build.postprocessing", true).get()

val KotlinBuildProperties.relocation: Boolean get() = postProcessing

val KotlinBuildProperties.proguard: Boolean get() = postProcessing && booleanProperty("kotlin.build.proguard", isTeamcityBuild).get()

val KotlinBuildProperties.jarCompression: Boolean get() = booleanProperty("kotlin.build.jar.compression", isTeamcityBuild).get()

val KotlinBuildProperties.ignoreTestFailures: Boolean get() = booleanProperty("ignoreTestFailures", isTeamcityBuild).get()

val KotlinBuildProperties.disableWerror: Boolean
    get() = booleanProperty("kotlin.build.disable.werror").get() || useFir.get() || booleanProperty("test.progressive.mode").get()

val KotlinBuildProperties.generateModularizedConfigurations: Boolean
    get() = booleanProperty("kotlin.fir.modularized.mt.configurations", false).get()

val KotlinBuildProperties.generateFullPipelineConfigurations: Boolean
    get() = booleanProperty("kotlin.fir.modularized.fp.configurations", false).get()

val KotlinBuildProperties.pathToKotlinModularizedTestData: String?
    get() = stringProperty("kotlin.fir.modularized.testdata.kotlin").orNull

val KotlinBuildProperties.pathToIntellijModularizedTestData: String?
    get() = stringProperty("kotlin.fir.modularized.testdata.intellij").orNull

val KotlinBuildProperties.pathToYoutrackModularizedTestData: String?
    get() = stringProperty("kotlin.fir.modularized.testdata.youtrack").orNull

val KotlinBuildProperties.pathToSpaceModularizedTestData: String?
    get() = stringProperty("kotlin.fir.modularized.testdata.space").orNull

val KotlinBuildProperties.isNativeRuntimeDebugInfoEnabled: Boolean
    get() = booleanProperty("kotlin.native.isNativeRuntimeDebugInfoEnabled", false).get()

val KotlinBuildProperties.junit5NumberOfThreadsForParallelExecution: Int?
    get() = intProperty("kotlin.test.junit5.maxParallelForks").orNull

val KotlinBuildProperties.useFirWithLightTree: Boolean
    get() = booleanProperty("kotlin.build.useFirLT").get()

val KotlinBuildProperties.useFirTightIC: Boolean
    get() = booleanProperty("kotlin.build.useFirIC").get()

val KotlinBuildProperties.isApplePrivacyManifestsPluginEnabled: Boolean
    get() = booleanProperty("kotlin.apple.applePrivacyManifestsPlugin", false).get()

val KotlinBuildProperties.limitTestTasksConcurrency: Boolean
    get() = booleanProperty("kotlin.build.limitTestTasksConcurrency", true).get()

val KotlinBuildProperties.konanDataDir: String?
    get() = stringProperty("konan.data.dir").orNull

/**
 * If `true`, `:kotlin-native:platformLibs` will compile platform libraries klibs without parallelism.
 */
val KotlinBuildProperties.limitPlatformLibsCompilationConcurrency: Boolean
    get() = !booleanProperty("kotlin.native.platformLibs.parallel", true).get()


/**
 * If `true`, `:kotlin-native:platformLibs` will build platform libraries caches without parallelism.
 */
val KotlinBuildProperties.limitPlatformLibsCacheBuildingConcurrency: Boolean
    get() {
        // if platform libs compilation parallelism is disabled, also disable parallel cache building by default.
        return !booleanProperty("kotlin.native.platformLibs.parallelCaches", !limitPlatformLibsCompilationConcurrency).get()
    }

/**
 * If `true`, `:kotlin-native:platformLibs` should be built with the bootstrap compiler.
 * Otherwise, they will be built with the snapshot compiler (i.e. built from the current sources)
 * Default: `false`.
 */
val KotlinBuildProperties.buildPlatformLibsByBootstrapCompiler: Boolean
    get() = booleanProperty("kotlin.native.platformLibs.bootstrap", false).get()

/**
 * "versions.kotlin-native" is the version of K/N dist that will be baked into KGP and that KGP will try to resolve to run K/N
 * compilations (including in KGP integration tests).
 *
 * We normally always want the K/N version to be aligned in TC builds. Though by default K/N is currently disabled, so when you run the
 * builds locally, the K/N version can be unaligned. In some TC builds we just want to run KGP integration tests quickly and check the
 * local DevX is not broken by stale K/N, so we disable the alignment in these builds.
 */
val KotlinBuildProperties.alignKotlinNativeVersionInTCBuilds: Boolean
    get() = isTeamcityBuild.get() && booleanProperty("kotlin.align.versions.kotlin-native.in.tc.builds", true).get()