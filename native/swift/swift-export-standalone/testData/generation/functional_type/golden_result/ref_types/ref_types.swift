@_implementationOnly import KotlinBridges_ref_types
import KotlinRuntimeSupport
import data

public func produce_block_with_reftype() -> (data.Foo, data.Bar) -> data.Foo {
    return {
        let nativeBlock = __root___produce_block_with_reftype()
        return { arg0, arg1 in
            let result = nativeBlock!(NSNumber(value: arg0.__externalRCRef()), NSNumber(value: arg1.__externalRCRef()))
            return data.Foo(__externalRCRef: result!.uintValue)
        }
    }()
}
