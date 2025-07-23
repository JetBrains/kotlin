@_implementationOnly import KotlinBridges_ref_types
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_opt_reftype(
    block: @escaping (Swift.Optional<Swift.Int32>, Swift.Optional<data.Bar>, Swift.Optional<Swift.String>, Swift.Optional<Swift.Set<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Optional<data.Foo>
) -> Swift.Void {
    return __root___consume_block_with_opt_reftype__TypesOfArguments__U28Swift_Optional_Swift_Int32__U20Swift_Optional_data_Bar__U20Swift_Optional_Swift_String__U20Swift_Optional_Swift_Set_anyU20KotlinRuntimeSupport__KotlinBridgeable__U29202D_U20Swift_Optional_data_Foo___({
        let originalBlock = block
        return { arg0, arg1, arg2, arg3 in return originalBlock(arg0.map { it in it.int32Value }, { switch arg1 { case nil: .none; case let res: data.Bar.__createClassWrapper(externalRCRef: res); } }(), arg2, arg3.map { it in it as! Swift.Set<any KotlinRuntimeSupport._KotlinBridgeable> }).map { it in it.__externalRCRef() } ?? nil }
    }())
}
public func consume_block_with_reftype_consumer(
    block: @escaping (data.Foo) -> Swift.Void
) -> Swift.Void {
    return __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0 in return originalBlock(data.Foo.__createClassWrapper(externalRCRef: arg0)) }
    }())
}
public func consume_block_with_reftype_factory(
    block: @escaping () -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__({
        let originalBlock = block
        return { return originalBlock().__externalRCRef() }
    }()))
}
public func consume_block_with_reftype_unzip(
    block: @escaping (data.Bar) -> data.Foo
) -> data.Foo {
    return data.Foo.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__({
        let originalBlock = block
        return { arg0 in return originalBlock(data.Bar.__createClassWrapper(externalRCRef: arg0)).__externalRCRef() }
    }()))
}
public func consume_block_with_reftype_zip(
    block: @escaping (data.Foo, data.Foo) -> data.Bar
) -> data.Bar {
    return data.Bar.__createClassWrapper(externalRCRef: __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__({
        let originalBlock = block
        return { arg0, arg1 in return originalBlock(data.Foo.__createClassWrapper(externalRCRef: arg0), data.Foo.__createClassWrapper(externalRCRef: arg1)).__externalRCRef() }
    }()))
}
