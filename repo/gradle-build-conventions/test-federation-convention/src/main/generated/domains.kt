// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'
package org.jetbrains.kotlin.testFederation

enum class Domain {
    Compiler,
    Frontend,
    CommonBackend,
    Jvm,
    Wasm,
    Js,
    Native,
    CoreLibs,
    AnalysisApi,
    BuildToolsApi,
    SwiftExport,
    CompilerPlugins,
    Gradle,
    Maven,
    IntelliJ,
    BuildInfrastructure,
    Unknown,
    ;

    companion object
}

internal object CompilerDomainInfo : DomainInfo {
    override val domain = Domain.Compiler
    override val include: List<String> = listOf("build-common", "compiler/*.kts", "compiler/*.md", "compiler/arguments", "compiler/arguments.common", "compiler/cli", "compiler/compiler.version", "compiler/config", "compiler/config.jvm", "compiler/container", "compiler/preloader", "compiler/test-infrastructure*", "compiler/test-security-manager", "compiler/testData", "compiler/testFixtures", "compiler/testResources", "compiler/tests", "compiler/tests-common*", "compiler/tests-compiler-utils", "compiler/tests-gen", "compiler/tests-integration", "compiler/tests-mutes", "compiler/util", "compiler/util-io", "core", "jps")
    override val exclude: List<String> = listOf("compiler/cli/cli-jklib")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo) }
}

internal object FrontendDomainInfo : DomainInfo {
    override val domain = Domain.Frontend
    override val include: List<String> = listOf("compiler/fir", "compiler/frontend*", "compiler/ir/ir.psi2ir", "compiler/java-direct", "compiler/javac-wrapper", "compiler/multiplatform-parsing", "compiler/psi/parser", "compiler/resolution*", "compiler/serialization*", "compiler/tests-java8", "compiler/tests-spec", "thiswontmatch")
    override val exclude: List<String> = listOf("thisWontEither")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object CommonBackendDomainInfo : DomainInfo {
    override val domain = Domain.CommonBackend
    override val include: List<String> = listOf("compiler/cli/cli-jklib", "compiler/ir/backend.common", "compiler/ir/ir.actualization", "compiler/ir/ir.inline", "compiler/ir/ir.tree", "compiler/ir/ir.validation", "compiler/ir/serialization.common", "compiler/ir/serialization.jklib", "compiler/jklib.tests", "compiler/util-klib", "compiler/util-klib-abi", "compiler/util-klib-metadata")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object JvmDomainInfo : DomainInfo {
    override val domain = Domain.Jvm
    override val include: List<String> = listOf("compiler/android-tests", "compiler/backend", "compiler/backend.common.jvm", "compiler/ir/backend.jvm", "compiler/ir/serialization.jvm", "compiler/tests-different-jdk")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object WasmDomainInfo : DomainInfo {
    override val domain = Domain.Wasm
    override val include: List<String> = listOf("wasm", "compiler/ir/backend.wasm", "js/js.translator/testData")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object JsDomainInfo : DomainInfo {
    override val domain = Domain.Js
    override val include: List<String> = listOf("js", "libraries/tools/analysis-api-based-klib-reader", "compiler/ir/backend.js", "compiler/ir/serialization.js")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object NativeDomainInfo : DomainInfo {
    override val domain = Domain.Native
    override val include: List<String> = listOf("native", "kotlin-native", "compiler/ir/backend.native", "compiler/ir/ir.objcinterop", "compiler/ir/serialization.native")
    override val exclude: List<String> = listOf("native/swift")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object CoreLibsDomainInfo : DomainInfo {
    override val domain = Domain.CoreLibs
    override val include: List<String> = listOf("libraries/stdlib", "libraries/tools/kotlin-annotations-jvm", "core/metadata*", "core/reflect*", "core/descriptors.runtime", "libraries/kotlinx-metadata", "libraries/reflect", "libraries/kotlin.test", "libraries/tools/jdk-api-validator")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object AnalysisApiDomainInfo : DomainInfo {
    override val domain = Domain.AnalysisApi
    override val include: List<String> = listOf("analysis", "compiler/psi", "prepare/analysis-api")
    override val exclude: List<String> = listOf("compiler/psi/parser")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object BuildToolsApiDomainInfo : DomainInfo {
    override val domain = Domain.BuildToolsApi
    override val include: List<String> = listOf("build-common", "compiler/build-tools", "compiler/incremental-compilation-*", "compiler/daemon", "compiler/compiler-runner-unshaded")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object SwiftExportDomainInfo : DomainInfo {
    override val domain = Domain.SwiftExport
    override val include: List<String> = listOf("native/swift", "libraries/tools/analysis-api-based-klib-reader")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(AnalysisApiDomainInfo) }
}

internal object CompilerPluginsDomainInfo : DomainInfo {
    override val domain = Domain.CompilerPlugins
    override val include: List<String> = listOf("plugins", "compiler/plugin-api")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object GradleDomainInfo : DomainInfo {
    override val domain = Domain.Gradle
    override val include: List<String> = listOf("build-common", "libraries/tools/*gradle*", "compiler/build-tools/kotlin-build-statistics")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object MavenDomainInfo : DomainInfo {
    override val domain = Domain.Maven
    override val include: List<String> = listOf("libraries/tools/*maven*")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object IntelliJDomainInfo : DomainInfo {
    override val domain = Domain.IntelliJ
    override val include: List<String> = listOf("prepare/ide-plugin-dependencies")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, AnalysisApiDomainInfo, CoreLibsDomainInfo) }
}

internal object BuildInfrastructureDomainInfo : DomainInfo {
    override val domain = Domain.BuildInfrastructure
    override val include: List<String> = listOf("repo", "gradle", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "scripts", ".space", ".idea")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object UnknownDomainInfo : DomainInfo {
    override val domain = Domain.Unknown
    override val include: List<String> = listOf()
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}


internal val allDomainInfos: List<DomainInfo> by lazy {
    listOf(
        CompilerDomainInfo,
        FrontendDomainInfo,
        CommonBackendDomainInfo,
        JvmDomainInfo,
        WasmDomainInfo,
        JsDomainInfo,
        NativeDomainInfo,
        CoreLibsDomainInfo,
        AnalysisApiDomainInfo,
        BuildToolsApiDomainInfo,
        SwiftExportDomainInfo,
        CompilerPluginsDomainInfo,
        GradleDomainInfo,
        MavenDomainInfo,
        IntelliJDomainInfo,
        BuildInfrastructureDomainInfo,
        UnknownDomainInfo,
    )
}