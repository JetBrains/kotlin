@_implementationOnly import KotlinBridges_ref_types
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_opt_reftype(
    block: @escaping (Swift.Int32?, data.Bar?, Swift.String?, Swift.Set<Swift.AnyHashable>?) -> data.Foo?
) -> Swift.Void {
    return { __root___consume_block_with_opt_reftype__TypesOfArguments__U28Swift_Optional_Swift_Int32__U20Swift_Optional_data_Bar__U20Swift_Optional_Swift_String__U20Swift_Optional_Swift_Set_Swift_AnyHashable__U29202D_U20Swift_Optional_data_Foo___(Unmanaged.passRetained((block as (Swift.Optional<Swift.Int32>, Swift.Optional<data.Bar>, Swift.Optional<Swift.String>, Swift.Optional<Swift.Set<Swift.AnyHashable>>) -> Swift.Optional<data.Foo>) as AnyObject).toOpaque()); return () }()
}
public func consume_block_with_reftype_consumer(
    block: @escaping (data.Foo) -> Swift.Void
) -> Swift.Void {
    return { __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__(Unmanaged.passRetained((block as (data.Foo) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func consume_block_with_reftype_factory(
    block: @escaping () -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__(Unmanaged.passRetained((block as () -> data.Foo) as AnyObject).toOpaque()))
}
public func consume_block_with_reftype_unzip(
    block: @escaping (data.Bar) -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__(Unmanaged.passRetained((block as (data.Bar) -> data.Foo) as AnyObject).toOpaque()))
}
public func consume_block_with_reftype_zip(
    block: @escaping (data.Foo, data.Foo) -> data.Bar
) -> data.Bar {
    return data.Bar.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__(Unmanaged.passRetained((block as (data.Foo, data.Foo) -> data.Bar) as AnyObject).toOpaque()))
}
@_cdecl("ref_types_internal_functional_type_callee_SwiftU2EOptionalU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Int32__Swift_Optional_data_Bar__Swift_Optional_Swift_String__Swift_Optional_Swift_Set_Swift_AnyHashable____")
package func ref_types_internal_functional_type_callee_SwiftU2EOptionalU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Int32__Swift_Optional_data_Bar__Swift_Optional_Swift_String__Swift_Optional_Swift_Set_Swift_AnyHashable____(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Foundation.NSNumber?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.String?, _ _4: Any?) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<data.Foo> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.Int32>, Swift.Optional<data.Bar>, Swift.Optional<Swift.String>, Swift.Optional<Swift.Set<Swift.AnyHashable>>) -> Swift.Optional<data.Foo>)(_1.map { it in it.int32Value }, { switch _2 { case nil: .none; case let res?: data.Bar.__createClassWrapper(externalRCRef: res); } }(), _3, _4.map { it in it as! Swift.Set<Swift.AnyHashable> })
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("ref_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo__")
package func ref_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (data.Foo) -> Swift.Void)(data.Foo.__createClassWrapper(externalRCRef: _1))
    return { _result; return true }()
}

@_cdecl("ref_types_internal_functional_type_callee_dataU2EBar__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo_data_Foo__")
package func ref_types_internal_functional_type_callee_dataU2EBar__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Foo_data_Foo__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: data.Bar = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (data.Foo, data.Foo) -> data.Bar)(data.Foo.__createClassWrapper(externalRCRef: _1), data.Foo.__createClassWrapper(externalRCRef: _2))
    return _result.__externalRCRef()
}

@_cdecl("ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: data.Foo = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> data.Foo)()
    return _result.__externalRCRef()
}

@_cdecl("ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Bar__")
package func ref_types_internal_functional_type_callee_dataU2EFoo__TypesOfArguments__Swift_UnsafeMutableRawPointer_data_Bar__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: data.Foo = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (data.Bar) -> data.Foo)(data.Bar.__createClassWrapper(externalRCRef: _1))
    return _result.__externalRCRef()
}
