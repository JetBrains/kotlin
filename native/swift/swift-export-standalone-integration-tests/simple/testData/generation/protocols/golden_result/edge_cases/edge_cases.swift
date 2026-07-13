@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_edge_cases
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Baz: KotlinRuntime.KotlinBase, edge_cases._Baz {
    func foo(
        result: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void
}
public protocol InterfaceA: KotlinRuntime.KotlinBase, edge_cases._InterfaceA {
    func foo() -> Swift.Void
}
public protocol InterfaceB: KotlinRuntime.KotlinBase, edge_cases.InterfaceA, edge_cases._InterfaceB {
    func bar() -> Swift.Void
}
@objc(_Baz)
public protocol _Baz {
}
@objc(_InterfaceA)
public protocol _InterfaceA {
}
@objc(_InterfaceB)
public protocol _InterfaceB: edge_cases._InterfaceA {
}
public protocol __Baz: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __InterfaceA: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol __InterfaceB: KotlinRuntimeSupport._KotlinBridgeable, edge_cases.__InterfaceA {
}
open class ClassC: KotlinRuntime.KotlinBase, edge_cases.InterfaceB, edge_cases.__InterfaceB {
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public final func baz() -> Swift.Void {
        return { ClassC_baz(self.__externalRCRef()); return () }()
    }
}
public final class _ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = conflictingTypealiases_Bar_Conflict_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { conflictingTypealiases_Bar_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = conflictingTypealiases_Foo_Conflict_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { conflictingTypealiases_Foo_Conflict_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
extension ExportedKotlinPackages.conflictingTypealiases.Bar where Self : ExportedKotlinPackages.conflictingTypealiases.__Bar {
}
extension ExportedKotlinPackages.conflictingTypealiases.Bar {
    typealias Conflict = edge_cases._ExportedKotlinPackages_conflictingTypealiases_Bar_Conflict
}
extension edge_cases.Baz where Self : edge_cases.__Baz {
    public func foo(
        result: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        return { Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), result.__externalRCRef()); return () }()
    }
}
extension edge_cases.Baz {
}
extension ExportedKotlinPackages.conflictingTypealiases.Foo where Self : ExportedKotlinPackages.conflictingTypealiases.__Foo {
}
extension ExportedKotlinPackages.conflictingTypealiases.Foo {
    typealias Conflict = edge_cases._ExportedKotlinPackages_conflictingTypealiases_Foo_Conflict
}
extension edge_cases.InterfaceA where Self : edge_cases.__InterfaceA {
    public func foo() -> Swift.Void {
        return { InterfaceA_foo(self.__externalRCRef()); return () }()
    }
}
extension edge_cases.InterfaceA {
}
extension edge_cases.InterfaceB where Self : edge_cases.__InterfaceB {
    public func bar() -> Swift.Void {
        return { InterfaceB_bar(self.__externalRCRef()); return () }()
    }
}
extension edge_cases.InterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistential: edge_cases.InterfaceA, edge_cases.__InterfaceA where Wrapped : edge_cases._InterfaceA {
}
extension KotlinRuntimeSupport._KotlinExistential: edge_cases.InterfaceB, edge_cases.__InterfaceB where Wrapped : edge_cases._InterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistential: edge_cases.Baz, edge_cases.__Baz where Wrapped : edge_cases._Baz {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.conflictingTypealiases.Foo, ExportedKotlinPackages.conflictingTypealiases.__Foo where Wrapped : ExportedKotlinPackages.conflictingTypealiases._Foo {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.conflictingTypealiases.Bar, ExportedKotlinPackages.conflictingTypealiases.__Bar where Wrapped : ExportedKotlinPackages.conflictingTypealiases._Bar {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: edge_cases._InterfaceA {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: edge_cases._InterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: edge_cases._Baz {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.conflictingTypealiases._Foo {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.conflictingTypealiases._Bar {
}
extension ExportedKotlinPackages.conflictingTypealiases {
    public protocol Bar: KotlinRuntime.KotlinBase, ExportedKotlinPackages.conflictingTypealiases.Foo, ExportedKotlinPackages.conflictingTypealiases._Bar {
    }
    public protocol Foo: KotlinRuntime.KotlinBase, ExportedKotlinPackages.conflictingTypealiases._Foo {
    }
    @objc(_Bar)
    public protocol _Bar: ExportedKotlinPackages.conflictingTypealiases._Foo {
    }
    @objc(_Foo)
    public protocol _Foo {
    }
    public protocol __Bar: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.conflictingTypealiases.__Foo {
    }
    public protocol __Foo: KotlinRuntimeSupport._KotlinBridgeable {
    }
}
@_cdecl("Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
package func Baz_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ result: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any edge_cases.Baz
    let _result: Swift.Void = _self.foo(result: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: result))
    return { _result; return true }()
}

@_cdecl("InterfaceA_foo__reverse_swift")
package func InterfaceA_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any edge_cases.InterfaceA
    let _result: Swift.Void = _self.foo()
    return { _result; return true }()
}

@_cdecl("InterfaceB_bar__reverse_swift")
package func InterfaceB_bar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any edge_cases.InterfaceB
    let _result: Swift.Void = _self.bar()
    return { _result; return true }()
}
