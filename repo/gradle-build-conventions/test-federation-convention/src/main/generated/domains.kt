// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'
package org.jetbrains.kotlin.testFederation

enum class Domain {
    CompilerInfrastructure,
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

internal object CompilerInfrastructureDomainInfo : DomainInfo {
    override val domain = Domain.CompilerInfrastructure
    override val include: List<String> = listOf("core", "build-common", "compiler/arguments", "compiler/arguments.common", "compiler/cli/bin", "compiler/arguments.common", "compiler/cli/cli-arguments-generator", "compiler/cli/cli-base", "compiler/cli/cli-js", "compiler/cli/cli-jvm", "compiler/cli/cli-metadata", "compiler/cli/cli-native-klib", "compiler/cli/cli-runner", "compiler/cli/src", "compiler/cli/resources", "compiler/cli/build.gradle.kts", "compiler/compiler.version", "compiler/config", "compiler/config.jvm", "compiler/container", "compiler/preloader", "compiler/test-infrastructure", "compiler/test-infrastructure-utils", "compiler/test-infrastructure-utils.common", "compiler/testResources", "compiler/testData", "compiler/tests", "compiler/tests-common", "compiler/tests-common-new", "compiler/tests-compiler-utils", "compiler/tests-integration", "compiler/tests-mutes", "compiler/util", "compiler/util-io", "compiler/build.gradle.kts", "jps")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo) }
}

internal object FrontendDomainInfo : DomainInfo {
    override val domain = Domain.Frontend
    override val include: List<String> = listOf("compiler/fir", "compiler/frontend", "compiler/frontend.common", "compiler/frontend.common.jvm", "compiler/frontend.common-psi", "compiler/frontend.java", "compiler/ir/ir.psi2ir", "compiler/java-direct", "compiler/multiplatform-parsing", "compiler/psi/parser", "compiler/resolution", "compiler/resolution.common", "compiler/resolution.common.jvm", "compiler/serialization", "compiler/serialization.common", "compiler/tests-java8", "compiler/tests-spec")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo) }
}

internal object CommonBackendDomainInfo : DomainInfo {
    override val domain = Domain.CommonBackend
    override val include: List<String> = listOf("compiler/ir/ir.actualization", "compiler/ir/ir.inline", "compiler/ir/ir.tree", "compiler/ir/ir.validation", "compiler/ir/serialization.common", "compiler/ir/backend.common", "compiler/util-klib", "compiler/util-klib-abi", "compiler/util-klib-metadata", "compiler/cli/cli-jklib", "compiler/jklib.tests", "compiler/ir/serialization.jklib")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo, FrontendDomainInfo) }
}

internal object JvmDomainInfo : DomainInfo {
    override val domain = Domain.Jvm
    override val include: List<String> = listOf("compiler/android-tests", "compiler/backend", "compiler/backend.common.jvm", "compiler/ir/backend.jvm", "compiler/ir/serialization.jvm", "compiler/tests-different-jdk")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo, FrontendDomainInfo, CommonBackendDomainInfo) }
}

internal object WasmDomainInfo : DomainInfo {
    override val domain = Domain.Wasm
    override val include: List<String> = listOf("compiler/ir/backend.wasm", "wasm", "js/js.translator/testData", "js/js.sourcemap", "js/typescript-export-model", "js/typescript-printer", "libraries/tools/dukat", "js/js.tests/testFixtures/org/jetbrains/kotlin", "js/js.config/src/org/jetbrains/kotlin/js/config")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo, FrontendDomainInfo, CommonBackendDomainInfo) }
}

internal object JsDomainInfo : DomainInfo {
    override val domain = Domain.Js
    override val include: List<String> = listOf("js", "compiler/ir/backend.js", "compiler/ir/serialization.js", "libraries/tools/analysis-api-based-klib-reader", "libraries/tools/dukat")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo, FrontendDomainInfo, CommonBackendDomainInfo) }
}

internal object NativeDomainInfo : DomainInfo {
    override val domain = Domain.Native
    override val include: List<String> = listOf("compiler/ir/backend.native", "compiler/ir/ir.objcinterop", "compiler/ir/serialization.native", "native", "kotlin-native")
    override val exclude: List<String> = listOf("native/swift")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo, CompilerInfrastructureDomainInfo, FrontendDomainInfo, CommonBackendDomainInfo) }
}

internal object CoreLibsDomainInfo : DomainInfo {
    override val domain = Domain.CoreLibs
    override val include: List<String> = listOf("libraries/stdlib", "libraries/tools/kotlin-annotations-jvm", "core/metadata*", "core/reflect*", "core/descriptors.runtime", "libraries/kotlinx-metadata", "libraries/reflect", "libraries/kotlin.test", "libraries/tools/jdk-api-validator")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object AnalysisApiDomainInfo : DomainInfo {
    override val domain = Domain.AnalysisApi
    override val include: List<String> = listOf("analysis", "compiler/psi", "prepare/analysis-api", "plugins/plugin-sandbox", "plugins/scripting")
    override val exclude: List<String> = listOf("compiler/psi/parser")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerInfrastructureDomainInfo, FrontendDomainInfo, CoreLibsDomainInfo) }
}

internal object BuildToolsApiDomainInfo : DomainInfo {
    override val domain = Domain.BuildToolsApi
    override val include: List<String> = listOf("build-common", "compiler/build-tools", "compiler/incremental-compilation-*", "compiler/daemon", "compiler/compiler-runner", "compiler/compiler-runner-unshaded")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerInfrastructureDomainInfo) }
}

internal object SwiftExportDomainInfo : DomainInfo {
    override val domain = Domain.SwiftExport
    override val include: List<String> = listOf("native/swift", "libraries/tools/analysis-api-based-klib-reader")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(AnalysisApiDomainInfo) }
}

internal object CompilerPluginsDomainInfo : DomainInfo {
    override val domain = Domain.CompilerPlugins
    override val include: List<String> = listOf("compiler/plugin-api", "plugins", "libraries/tools/kotlin-main-kts", "libraries/tools/kotlin-main-kts-test", "libraries/scripting")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerInfrastructureDomainInfo, FrontendDomainInfo, CommonBackendDomainInfo, CoreLibsDomainInfo) }
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
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerInfrastructureDomainInfo, FrontendDomainInfo, AnalysisApiDomainInfo, CoreLibsDomainInfo, BuildToolsApiDomainInfo, CompilerPluginsDomainInfo) }
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
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerInfrastructureDomainInfo) }
}


val allDomainInfos: List<DomainInfo> by lazy {
    listOf(
        CompilerInfrastructureDomainInfo,
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