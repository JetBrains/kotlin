@_implementationOnly import KotlinBridges_f_bounded_type
import KotlinRuntime
import KotlinRuntimeSupport

public protocol MyComparable: KotlinRuntime.KotlinBase, f_bounded_type._MyComparable {
    func compareTo(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32
}
@objc(_MyComparable)
public protocol _MyComparable {
}
public final class ConcreteSelfReferencing: f_bounded_type.SelfReferencing {
    public override init() {
        let __kt = __root___ConcreteSelfReferencing_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
open class SelfReferencing: KotlinRuntime.KotlinBase {
    public init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func compareTo(
        other: f_bounded_type.SelfReferencing
    ) -> Swift.Int32 {
        return SelfReferencing_compareTo__TypesOfArguments__f_bounded_type_SelfReferencing__(self.__externalRCRef(), other.__externalRCRef())
    }
}
extension f_bounded_type.MyComparable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func compareTo(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return MyComparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension f_bounded_type.MyComparable {
}
extension KotlinRuntimeSupport._KotlinExistential: f_bounded_type.MyComparable where Wrapped : f_bounded_type._MyComparable {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: f_bounded_type._MyComparable {
}
@_cdecl("MyComparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
public func MyComparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any f_bounded_type.MyComparable
    let _result: Swift.Int32 = _self.compareTo(other: { switch other { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("SelfReferencing_compareTo__TypesOfArguments__f_bounded_type_SelfReferencing____reverse_swift")
public func SelfReferencing_compareTo__TypesOfArguments__f_bounded_type_SelfReferencing____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any f_bounded_type.MyComparable
    let _result: Swift.Int32 = _self.compareTo(other: f_bounded_type.SelfReferencing.__createClassWrapper(externalRCRef: other))
    return _result
}
