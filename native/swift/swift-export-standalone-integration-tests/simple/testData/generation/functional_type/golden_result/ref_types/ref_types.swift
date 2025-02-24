@_implementationOnly import KotlinBridges_ref_types
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_reftype_consumer(
    block: @escaping @convention(block) (data.Foo) -> Swift.Void
) -> Swift.Void {
    return __root___consume_block_with_reftype_consumer__TypesOfArguments__U28data_FooU29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0 in return originalBlock(data.Foo(__externalRCRef: arg0)) }
    }())
}
public func consume_block_with_reftype_factory(
    block: @escaping @convention(block) () -> data.Foo
) -> data.Foo {
    return data.Foo(__externalRCRef: __root___consume_block_with_reftype_factory__TypesOfArguments__U2829202D_U20data_Foo__({
        let originalBlock = block
        return { return originalBlock().__externalRCRef() }
    }()))
}
public func consume_block_with_reftype_unzip(
    block: @escaping @convention(block) (data.Bar) -> data.Foo
) -> data.Foo {
    return data.Foo(__externalRCRef: __root___consume_block_with_reftype_unzip__TypesOfArguments__U28data_BarU29202D_U20data_Foo__({
        let originalBlock = block
        return { arg0 in return originalBlock(data.Bar(__externalRCRef: arg0)).__externalRCRef() }
    }()))
}
public func consume_block_with_reftype_zip(
    block: @escaping @convention(block) (data.Foo, data.Foo) -> data.Bar
) -> data.Bar {
    return data.Bar(__externalRCRef: __root___consume_block_with_reftype_zip__TypesOfArguments__U28data_Foo_U20data_FooU29202D_U20data_Bar__({
        let originalBlock = block
        return { arg0, arg1 in return originalBlock(data.Foo(__externalRCRef: arg0), data.Foo(__externalRCRef: arg1)).__externalRCRef() }
    }()))
}
