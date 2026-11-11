@_implementationOnly import KotlinBridges_f_bounded_type
import KotlinRuntime
import KotlinRuntimeSupport

public protocol MyComparable<T>: KotlinRuntime.KotlinBase, f_bounded_type._MyComparable {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    func compareTo(
        other: T
    ) -> Swift.Int32
}
@objc(_f_bounded_type_MyComparable)
public protocol _MyComparable {
}
@_documentation(visibility: internal)
public protocol _SelfReferencing_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    func compareTo(
        other: T
    ) -> Swift.Int32
}
public protocol __MyComparable: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class AnyMyComparable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, f_bounded_type.MyComparable, f_bounded_type.__MyComparable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class BaseClass<T: f_bounded_type.MyComparable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable where T.T == T {
    @inlinable
    public var v: T {
        get {
            return T.__fromKotlinPointer(_cbridge_BaseClass_v_get(self.__externalRCRef()))
        }
        set {
            return { _cbridge_BaseClass_v_set__TypesOfArguments__T_f_bounded_type_MyComparable__(self.__externalRCRef(), newValue.__retainedToKotlinPointer()); return () }()
        }
    }
    public init(
        v: T
    ) {
        let __kt = __root___BaseClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
        { __root___BaseClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T_f_bounded_type_MyComparable__(__kt, v.__retainedToKotlinPointer()); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ConcreteSelfReferencing: f_bounded_type.SelfReferencing<KotlinRuntime.KotlinBase> {
    public override init() {
        let __kt = __root___ConcreteSelfReferencing_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ConcreteSelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
open class SelfReferencing<T: KotlinRuntimeSupport._KotlinBridgeable>: f_bounded_type._SelfReferencing_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, f_bounded_type._SelfReferencing_ReverseDispatch, f_bounded_type.MyComparable, f_bounded_type.__MyComparable {
    public init() {
         let __kt: Swift.UnsafeMutableRawPointer!
         if Self.self == f_bounded_type.SelfReferencing<T>.self {
             __kt = __root___SelfReferencing_init_allocate()
         } else {
             __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
         }
        super.init(__externalRCRefUnsafe: __kt, options: Self.self == f_bounded_type.SelfReferencing<T>.self ? .asExistentialWrapper : .asBoundBridge);
        { __root___SelfReferencing_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func compareTo(
        other: T
    ) -> Swift.Int32 {
        if Self.self == f_bounded_type.SelfReferencing<T>.self {
            return SelfReferencing_compareTo__TypesOfArguments__T__(self.__externalRCRef(), other.__retainedToKotlinPointer())
        } else {
            return SelfReferencing_compareTo__TypesOfArguments__T___direct(self.__externalRCRef(), other.__retainedToKotlinPointer())
        }
    }
}
@_documentation(visibility: internal)
open class _SelfReferencing_Base: KotlinRuntime.KotlinBase {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@inlinable
public func testFun<T: f_bounded_type.MyComparable>(
    a: T
) -> T where T.T == T {
    return T.__fromKotlinPointer(_cbridge___root___testFun__TypesOfArguments__T_f_bounded_type_MyComparable__(a.__retainedToKotlinPointer()))
}
@_documentation(visibility: internal)
extension f_bounded_type.MyComparable where Self : f_bounded_type.__MyComparable {
    public func compareTo(
        other: T
    ) -> Swift.Int32 {
        return MyComparable_compareTo__TypesOfArguments__T__(self.__externalRCRef(), other.__retainedToKotlinPointer())
    }
}
extension f_bounded_type.MyComparable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: f_bounded_type._MyComparable {
}
@_cdecl("MyComparable_compareTo__TypesOfArguments__T____reverse_swift")
package func MyComparable_compareTo__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any f_bounded_type.MyComparable)
    func _open<__Self: f_bounded_type.MyComparable>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.compareTo(other: __Self.T.__fromKotlinPointer(other))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("SelfReferencing_compareTo__TypesOfArguments__T____reverse_swift")
package func SelfReferencing_compareTo__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any f_bounded_type._SelfReferencing_ReverseDispatch)
    func _open<__Self: f_bounded_type._SelfReferencing_ReverseDispatch>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.compareTo(other: __Self.T.__fromKotlinPointer(other))
        return _result
    }
    return _open(_rawSelf)
}

@usableFromInline
@discardableResult
internal func _cbridge_BaseClass_v_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return BaseClass_v_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_BaseClass_v_set__TypesOfArguments__T_f_bounded_type_MyComparable__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    return BaseClass_v_set__TypesOfArguments__T_f_bounded_type_MyComparable__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge___root___testFun__TypesOfArguments__T_f_bounded_type_MyComparable__(_ _p0: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    return __root___testFun__TypesOfArguments__T_f_bounded_type_MyComparable__(_p0)
}
