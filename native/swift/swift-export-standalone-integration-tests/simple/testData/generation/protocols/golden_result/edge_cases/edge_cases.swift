@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_edge_cases
import KotlinRuntime
import KotlinRuntimeSupport

public final class _ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != edge_cases._ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from edge_cases._ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict ") }
        let __kt = conflictingTypealiases_Bar_Conflict_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class _ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != edge_cases._ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from edge_cases._ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict ") }
        let __kt = conflictingTypealiases_Foo_Conflict_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
extension ExportedKotlinPackages.conflictingTypealiases.Bar where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.conflictingTypealiases.Bar {
    typealias Conflict = edge_cases._ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict
}
extension ExportedKotlinPackages.conflictingTypealiases.Foo where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.conflictingTypealiases.Foo {
    typealias Conflict = edge_cases._ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.conflictingTypealiases.Foo where Wrapped : ExportedKotlinPackages.conflictingTypealiases._Foo {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.conflictingTypealiases.Bar where Wrapped : ExportedKotlinPackages.conflictingTypealiases._Bar {
}
extension ExportedKotlinPackages.conflictingTypealiases {
    public protocol Bar: KotlinRuntime.KotlinBase, ExportedKotlinPackages.conflictingTypealiases.Foo {
    }
    public protocol Foo: KotlinRuntime.KotlinBase {
    }
    @objc(_Bar)
    package protocol _Bar: ExportedKotlinPackages.conflictingTypealiases._Foo {
    }
    @objc(_Foo)
    package protocol _Foo {
    }
}
