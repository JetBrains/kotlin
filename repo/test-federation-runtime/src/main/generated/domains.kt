// This file is generated automatically. DO NOT MODIFY IT MANUALLY
// See GenerateTestFederationRuntimeCodeTask

package org.jetbrains.kotlin.testFederation

enum class Domain {
    Compiler,
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

}


internal val contractsByDomain = buildMap<Domain, Set<Domain>> {
    put(Domain.Compiler, buildSet {
        add(Domain.CoreLibs)
    })

    put(Domain.Wasm, buildSet {
        add(Domain.Compiler)
        add(Domain.CoreLibs)
    })

    put(Domain.Js, buildSet {
        add(Domain.Compiler)
        add(Domain.CoreLibs)
    })

    put(Domain.Native, buildSet {
        add(Domain.Compiler)
        add(Domain.CoreLibs)
    })

    put(Domain.AnalysisApi, buildSet {
        add(Domain.Compiler)
        add(Domain.CoreLibs)
    })

    put(Domain.BuildToolsApi, buildSet {
        add(Domain.Compiler)
    })

    put(Domain.SwiftExport, buildSet {
        add(Domain.AnalysisApi)
    })

    put(Domain.CompilerPlugins, buildSet {
        add(Domain.Compiler)
    })

    put(Domain.IntelliJ, buildSet {
        add(Domain.Compiler)
        add(Domain.AnalysisApi)
        add(Domain.CoreLibs)
        add(Domain.BuildToolsApi)
        add(Domain.CompilerPlugins)
    })

    put(Domain.Unknown, buildSet {
        add(Domain.Compiler)
    })

}

internal val contractedDomainsByTrigger = buildMap<Domain, Set<Domain>> {
    put(Domain.Compiler, buildSet {
        add(Domain.Wasm)
        add(Domain.Js)
        add(Domain.Native)
        add(Domain.AnalysisApi)
        add(Domain.BuildToolsApi)
        add(Domain.CompilerPlugins)
        add(Domain.IntelliJ)
        add(Domain.Unknown)
    })

    put(Domain.Wasm, emptySet())

    put(Domain.Js, emptySet())

    put(Domain.Native, emptySet())

    put(Domain.CoreLibs, buildSet {
        add(Domain.Compiler)
        add(Domain.Wasm)
        add(Domain.Js)
        add(Domain.Native)
        add(Domain.AnalysisApi)
        add(Domain.IntelliJ)
    })

    put(Domain.AnalysisApi, buildSet {
        add(Domain.SwiftExport)
        add(Domain.IntelliJ)
    })

    put(Domain.BuildToolsApi, buildSet {
        add(Domain.IntelliJ)
    })

    put(Domain.SwiftExport, emptySet())

    put(Domain.CompilerPlugins, buildSet {
        add(Domain.IntelliJ)
    })

    put(Domain.Gradle, emptySet())

    put(Domain.Maven, emptySet())

    put(Domain.IntelliJ, emptySet())

    put(Domain.BuildInfrastructure, emptySet())

    put(Domain.Unknown, emptySet())

}