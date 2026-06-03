// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'
package org.jetbrains.kotlin.testFederation

enum class Domain {
    Compiler,
    Wasm,
    Js,
    Native,
    CoreLibs,
    AnalysisApi,
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
    override val include: List<String> = listOf("compiler/**", "core/**", "build-common/**", "compiler/psi/parser/**", "plugins/plugin-sandbox/**", "plugins/scripting/**", "jps/**")
    override val exclude: List<String> = listOf("compiler/psi/**")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CoreLibsDomainInfo) }
}

internal object WasmDomainInfo : DomainInfo {
    override val domain = Domain.Wasm
    override val include: List<String> = listOf("wasm/**", "js/js.translator/testData/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object JsDomainInfo : DomainInfo {
    override val domain = Domain.Js
    override val include: List<String> = listOf("js/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object NativeDomainInfo : DomainInfo {
    override val domain = Domain.Native
    override val include: List<String> = listOf("native/**", "kotlin-native/**")
    override val exclude: List<String> = listOf("native/swift/**")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object CoreLibsDomainInfo : DomainInfo {
    override val domain = Domain.CoreLibs
    override val include: List<String> = listOf("libraries/stdlib/**", "libraries/reflect/**", "libraries/kotlin.test/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object AnalysisApiDomainInfo : DomainInfo {
    override val domain = Domain.AnalysisApi
    override val include: List<String> = listOf("analysis/**", "compiler/psi/**")
    override val exclude: List<String> = listOf("compiler/psi/parser/**")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, CoreLibsDomainInfo) }
}

internal object SwiftExportDomainInfo : DomainInfo {
    override val domain = Domain.SwiftExport
    override val include: List<String> = listOf("native/swift/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(AnalysisApiDomainInfo) }
}

internal object CompilerPluginsDomainInfo : DomainInfo {
    override val domain = Domain.CompilerPlugins
    override val include: List<String> = listOf("plugins/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, UnknownDomainInfo) }
}

internal object GradleDomainInfo : DomainInfo {
    override val domain = Domain.Gradle
    override val include: List<String> = listOf("libraries/tools/*gradle*/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object MavenDomainInfo : DomainInfo {
    override val domain = Domain.Maven
    override val include: List<String> = listOf("libraries/tools/*maven*/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object IntelliJDomainInfo : DomainInfo {
    override val domain = Domain.IntelliJ
    override val include: List<String> = listOf()
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, AnalysisApiDomainInfo, CoreLibsDomainInfo) }
}

internal object BuildInfrastructureDomainInfo : DomainInfo {
    override val domain = Domain.BuildInfrastructure
    override val include: List<String> = listOf("repo/**", "gradle/**", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "scripts/**", ".space/**", ".idea/**")
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
        WasmDomainInfo,
        JsDomainInfo,
        NativeDomainInfo,
        CoreLibsDomainInfo,
        AnalysisApiDomainInfo,
        SwiftExportDomainInfo,
        CompilerPluginsDomainInfo,
        GradleDomainInfo,
        MavenDomainInfo,
        IntelliJDomainInfo,
        BuildInfrastructureDomainInfo,
        UnknownDomainInfo,
    )
}