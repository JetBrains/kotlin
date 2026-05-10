// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See 'codegen.gradle.kts'
package org.jetbrains.kotlin.testFederation

enum class Domain {
    Compiler,
    Wasm,
    Js,
    Native,
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
    override val home = "compiler"
    override val domain = Domain.Compiler
    override val include: List<String> = listOf("compiler/**", "core/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(UnknownDomainInfo) }
}

internal object WasmDomainInfo : DomainInfo {
    override val home = "wasm"
    override val domain = Domain.Wasm
    override val include: List<String> = listOf("wasm/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object JsDomainInfo : DomainInfo {
    override val home = "js"
    override val domain = Domain.Js
    override val include: List<String> = listOf("js/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object NativeDomainInfo : DomainInfo {
    override val home = "kotlin-native"
    override val domain = Domain.Native
    override val include: List<String> = listOf("native/**", "kotlin-native/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object AnalysisApiDomainInfo : DomainInfo {
    override val home = "analysis"
    override val domain = Domain.AnalysisApi
    override val include: List<String> = listOf("analysis/**", "compiler/psi/**")
    override val exclude: List<String> = listOf("compiler/psi/parser/**")
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}

internal object SwiftExportDomainInfo : DomainInfo {
    override val home = "native/swift"
    override val domain = Domain.SwiftExport
    override val include: List<String> = listOf("native/swift/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object CompilerPluginsDomainInfo : DomainInfo {
    override val home = "plugins"
    override val domain = Domain.CompilerPlugins
    override val include: List<String> = listOf("plugins/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, UnknownDomainInfo) }
}

internal object GradleDomainInfo : DomainInfo {
    override val home = "libraries/tools/kotlin-gradle-plugin"
    override val domain = Domain.Gradle
    override val include: List<String> = listOf("libraries/tools/*gradle*/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object MavenDomainInfo : DomainInfo {
    override val home = "libraries/tools/kotlin-maven-plugin"
    override val domain = Domain.Maven
    override val include: List<String> = listOf("libraries/tools/*maven*/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object IntelliJDomainInfo : DomainInfo {
    override val home = "idea"
    override val domain = Domain.IntelliJ
    override val include: List<String> = listOf()
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo, AnalysisApiDomainInfo) }
}

internal object BuildInfrastructureDomainInfo : DomainInfo {
    override val home = "repo"
    override val domain = Domain.BuildInfrastructure
    override val include: List<String> = listOf("repo/**", "gradle/**", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "scripts/**")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf() }
}

internal object UnknownDomainInfo : DomainInfo {
    override val home = "."
    override val domain = Domain.Unknown
    override val include: List<String> = listOf("**/*")
    override val exclude: List<String> = listOf()
    override val fullyAffectedBy: List<DomainInfo> by lazy { listOf(CompilerDomainInfo) }
}


internal val allDomainInfos: List<DomainInfo> by lazy {
    listOf(
        CompilerDomainInfo,
        WasmDomainInfo,
        JsDomainInfo,
        NativeDomainInfo,
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