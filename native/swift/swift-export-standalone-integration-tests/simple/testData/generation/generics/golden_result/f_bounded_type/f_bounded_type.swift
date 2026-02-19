@_implementationOnly import KotlinBridges_f_bounded_type
import KotlinRuntime
import KotlinRuntimeSupport

public protocol MyComparable: KotlinRuntime.KotlinBase {
    func compareTo(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32
}
@objc(_MyComparable)
package protocol _MyComparable {
}
public final class ConcreteSelfReferencing: f_bounded_type.SelfReferencing {
    public override init() {
        if Self.self != f_bounded_type.ConcreteSelfReferencing.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from f_bounded_type.ConcreteSelfReferencing ") }
        let __kt = __root___ConcreteSelfReferencing_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
