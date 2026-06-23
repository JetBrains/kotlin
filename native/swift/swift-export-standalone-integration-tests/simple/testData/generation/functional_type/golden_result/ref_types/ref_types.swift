@_implementationOnly import KotlinBridges_ref_types
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_opt_reftype(
    block: @escaping (Swift.Int32?, data.Bar?, Swift.String?, Swift.Set<Swift.AnyHashable>?) -> data.Foo?
) -> Swift.Void {
    return { __root___consume_block_with_opt_reftype__TypesOfArguments__U28Swift_Optional_Swift_Int32__U20Swift_Optional_data_Bar__U20Swift_Optional_Swift_String__U20Swift_Optional_Swift_Set_Swift_AnyHashable__U29202D_U20Swift_Optional_data_Foo___({
        let originalBlock: (Swift.Optional<Swift.Int32>, Swift.Optional<data.Bar>, Swift.Optional<Swift.String>, Swift.Optional<Swift.Set<Swift.AnyHashable>>) -> Swift.Optional<data.Foo> = block
        return { (arg0: Foundation.NSNumber?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.String?, arg3: Any?) in
            let _arg0: Swift.Optional<Swift.Int32> = arg0.map { it in it.int32Value }
            let _arg1: Swift.Optional<data.Bar> = { switch arg1 { case nil: .none; case let res?: data.Bar.__createClassWrapper(externalRCRef: res); } }()
            let _arg2: Swift.Optional<Swift.String> = arg2
            let _arg3: Swift.Optional<Swift.Set<Swift.AnyHashable>> = arg3.map { it in it as! Swift.Set<Swift.AnyHashable> }
            let _result = originalBlock(_arg0, _arg1, _arg2, _arg3)
            return _result.map { it in it.__externalRCRef() } ?? nil
        }
    }()); return () }()
}
public func consume_block_with_reftype_consumer(
    block: @escaping (data.Foo) -> Swift.Void
) -> Swift.Void {
    return { __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__({
        let originalBlock: (data.Foo) -> Swift.Void = block
        return { (arg0: Swift.UnsafeMutableRawPointer) in
            let _arg0: data.Foo = data.Foo.__createClassWrapper(externalRCRef: arg0)
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }()); return () }()
}
public func consume_block_with_reftype_factory(
    block: @escaping () -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__({
        let originalBlock: () -> data.Foo = block
        return {
            let _result = originalBlock()
            return _result.__externalRCRef()
        }
    }()))
}
public func consume_block_with_reftype_unzip(
    block: @escaping (data.Bar) -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__({
        let originalBlock: (data.Bar) -> data.Foo = block
        return { (arg0: Swift.UnsafeMutableRawPointer) in
            let _arg0: data.Bar = data.Bar.__createClassWrapper(externalRCRef: arg0)
            let _result = originalBlock(_arg0)
            return _result.__externalRCRef()
        }
    }()))
}
public func consume_block_with_reftype_zip(
    block: @escaping (data.Foo, data.Foo) -> data.Bar
) -> data.Bar {
    return data.Bar.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__({
        let originalBlock: (data.Foo, data.Foo) -> data.Bar = block
        return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer) in
            let _arg0: data.Foo = data.Foo.__createClassWrapper(externalRCRef: arg0)
            let _arg1: data.Foo = data.Foo.__createClassWrapper(externalRCRef: arg1)
            let _result = originalBlock(_arg0, _arg1)
            return _result.__externalRCRef()
        }
    }()))
}
