@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import anotherFeature
import feature

public func bar() -> anotherFeature.FeatureC {
    return anotherFeature.FeatureC.__createClassWrapper(externalRCRef: __root___bar())
}
public func foo() -> ExportedKotlinPackages.oh.my.kotlin.FeatureA {
    return ExportedKotlinPackages.oh.my.kotlin.FeatureA.__createClassWrapper(externalRCRef: __root___foo())
}
