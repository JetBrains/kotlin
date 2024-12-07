/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val KotlinBuildProperties.includeJava9: Boolean
    get() = !isInJpsBuildIdeaSync && getBoolean("kotlin.build.java9", true)

val KotlinBuildProperties.useBootstrapStdlib: Boolean
    get() = isInJpsBuildIdeaSync || getBoolean("kotlin.build.useBootstrapStdlib", false)

val KotlinBuildProperties.postProcessing: Boolean get() = isTeamcityBuild || getBoolean("kotlin.build.postprocessing", true)

val KotlinBuildProperties.relocation: Boolean get() = postProcessing

val KotlinBuildProperties.proguard: Boolean get() = postProcessing && getBoolean("kotlin.build.proguard", isTeamcityBuild)

val KotlinBuildProperties.jarCompression: Boolean get() = getBoolean("kotlin.build.jar.compression", isTeamcityBuild)

val KotlinBuildProperties.ignoreTestFailures: Boolean get() = getBoolean("ignoreTestFailures", isTeamcityBuild)

val KotlinBuildProperties.disableWerror: Boolean
    get() = getBoolean("kotlin.build.disable.werror") || useFir || isInJpsBuildIdeaSync || getBoolean("test.progressive.mode")

val KotlinBuildProperties.generateModularizedConfigurations: Boolean
    get() = getBoolean("kotlin.fir.modularized.mt.configurations", false)

val KotlinBuildProperties.generateFullPipelineConfigurations: Boolean
    get() = getBoolean("kotlin.fir.modularized.fp.configurations", false)

val KotlinBuildProperties.pathToKotlinModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.kotlin") as? String

val KotlinBuildProperties.pathToIntellijModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.intellij") as? String

val KotlinBuildProperties.pathToYoutrackModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.youtrack") as? String

val KotlinBuildProperties.pathToSpaceModularizedTestData: String?
    get() = getOrNull("kotlin.fir.modularized.testdata.space") as? String

val KotlinBuildProperties.isNativeRuntimeDebugInfoEnabled: Boolean
    get() = getBoolean("kotlin.native.isNativeRuntimeDebugInfoEnabled", false)

val KotlinBuildProperties.junit5NumberOfThreadsForParallelExecution: Int?
    get() = (getOrNull("kotlin.test.junit5.maxParallelForks") as? String)?.toInt()

val KotlinBuildProperties.useFirWithLightTree: Boolean
    get() = getBoolean("kotlin.build.useFirLT")

val KotlinBuildProperties.useFirTightIC: Boolean
    get() = getBoolean("kotlin.build.useFirIC")

val KotlinBuildProperties.isApplePrivacyManifestsPluginEnabled: Boolean
    get() = getBoolean("kotlin.apple.applePrivacyManifestsPlugin", false)

val KotlinBuildProperties.limitTestTasksConcurrency: Boolean
    get() = getBoolean("kotlin.build.limitTestTasksConcurrency", true)

val KotlinBuildProperties.konanDataDir: String?
    get() = getOrNull("konan.data.dir") as String?

/**
 * If `true`, `:kotlin-native:platformLibs` will compile platform libraries klibs without parallelism.
 */
val KotlinBuildProperties.limitPlatformLibsCompilationConcurrency: Boolean
    get() = !getBoolean("kotlin.native.platformLibs.parallel", true)


/**
 * If `true`, `:kotlin-native:platformLibs` will build platform libraries caches without parallelism.
 */
val KotlinBuildProperties.limitPlatformLibsCacheBuildingConcurrency: Boolean
    get() {
        // if platform libs compilation parallelism is disabled, also disable parallel cache building by default.
        return !getBoolean("kotlin.native.platformLibs.parallelCaches", !limitPlatformLibsCompilationConcurrency)
    }