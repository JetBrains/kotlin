@_implementationOnly import KotlinRuntimeSupportBridge
import KotlinRuntime

public struct KotlinError: Error & CustomStringConvertible {
    public var wrapped: KotlinRuntime.KotlinBase

    public init(wrapped: KotlinRuntime.KotlinBase) {
        self.wrapped = wrapped
    }

    public var description: String {
        return __root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__(self.wrapped.__externalRCRef())
            ?? "KotlinException(\(self.wrapped.description))"
    }
}

public protocol _KotlinBridgeable {
    init(__externalRCRefUnsafe: UnsafeMutableRawPointer!, options: KotlinBaseConstructionOptions)
    func __externalRCRef() -> UnsafeMutableRawPointer!
}

public class _KotlinExistential<Wrapped>: KotlinBase {

}

extension KotlinBase : _KotlinBridgeable {
}

// MARK: - _KotlinBridgeable conformances for primitive types

extension Swift.Int8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int8_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Int8_box(self)
    }
}

extension Swift.Int16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int16_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Int16_box(self)
    }
}

extension Swift.Int32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int32_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Int32_box(self)
    }
}

extension Swift.Int64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int64_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Int64_box(self)
    }
}

extension Swift.UInt8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt8_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_UInt8_box(self)
    }
}

extension Swift.UInt16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt16_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_UInt16_box(self)
    }
}

extension Swift.UInt32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt32_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_UInt32_box(self)
    }
}

extension Swift.UInt64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt64_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_UInt64_box(self)
    }
}

extension Swift.Bool: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Bool_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Bool_box(self)
    }
}

extension Swift.Float: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Float_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Float_box(self)
    }
}

extension Swift.Double: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Double_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Double_box(self)
    }
}

// MARK: - _KotlinBridgeable conformance for String

extension Swift.String: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_String_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_String_box(self)
    }
}

// MARK: - _KotlinBridgeable conformances for collection types

extension Swift.Array: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Array_unbox(ref) as! [Element]
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Array_box(self)
    }
}

extension Swift.Set: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = Unmanaged<NSSet>.fromOpaque(KotlinBridgeable_Set_unbox(ref)).takeUnretainedValue() as! Set<Element>
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Set_box(Unmanaged.passUnretained(self as NSSet).toOpaque())
    }
}

extension Swift.Dictionary: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer!, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = Unmanaged<NSDictionary>.fromOpaque(KotlinBridgeable_Dictionary_unbox(ref)).takeUnretainedValue() as! [Key: Value]
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer! {
        return KotlinBridgeable_Dictionary_box(Unmanaged.passUnretained(self as NSDictionary).toOpaque())
    }
}

// MARK: - __createBridgeable: unwraps bridgeable types from externalRCRef

extension KotlinBase {
    public static func __createBridgeable(externalRCRef ref: UnsafeMutableRawPointer!) -> any _KotlinBridgeable {
        let tag = KotlinBridgeable_getTypeTag(ref)
        switch tag {
        case 1:  let v = KotlinBridgeable_String_unbox(ref);  KotlinBridgeable_disposeRef(ref); return v
        case 2:  let v = KotlinBridgeable_Int8_unbox(ref);    KotlinBridgeable_disposeRef(ref); return v
        case 3:  let v = KotlinBridgeable_Int16_unbox(ref);   KotlinBridgeable_disposeRef(ref); return v
        case 4:  let v = KotlinBridgeable_Int32_unbox(ref);   KotlinBridgeable_disposeRef(ref); return v
        case 5:  let v = KotlinBridgeable_Int64_unbox(ref);   KotlinBridgeable_disposeRef(ref); return v
        case 6:  let v = KotlinBridgeable_UInt8_unbox(ref);   KotlinBridgeable_disposeRef(ref); return v
        case 7:  let v = KotlinBridgeable_UInt16_unbox(ref);  KotlinBridgeable_disposeRef(ref); return v
        case 8:  let v = KotlinBridgeable_UInt32_unbox(ref);  KotlinBridgeable_disposeRef(ref); return v
        case 9:  let v = KotlinBridgeable_UInt64_unbox(ref);  KotlinBridgeable_disposeRef(ref); return v
        case 10: let v = KotlinBridgeable_Bool_unbox(ref);    KotlinBridgeable_disposeRef(ref); return v
        case 11: let v = KotlinBridgeable_Float_unbox(ref);   KotlinBridgeable_disposeRef(ref); return v
        case 12: let v = KotlinBridgeable_Double_unbox(ref);  KotlinBridgeable_disposeRef(ref); return v
        case 13: let v: [Any] = KotlinBridgeable_Array_unbox(ref) as! [Any]; KotlinBridgeable_disposeRef(ref); return v
        case 14:
            let v = Unmanaged<NSSet>.fromOpaque(KotlinBridgeable_Set_unbox(ref)).takeUnretainedValue() as! Set<AnyHashable>
            KotlinBridgeable_disposeRef(ref); return v
        case 15:
            let v = Unmanaged<NSDictionary>.fromOpaque(KotlinBridgeable_Dictionary_unbox(ref)).takeUnretainedValue() as! [AnyHashable: Any]
            KotlinBridgeable_disposeRef(ref); return v
        default:
            return __createProtocolWrapper(externalRCRef: ref) as! any _KotlinBridgeable
        }
    }
}

extension NSObject {
    // FIXME: swap to @expose(C) when it's available or consider using @expose(Cxx)
    @objc(_Kotlin_SwiftExport_wrapIntoExistential:)
    private static func _kotlinGetExistentialType(markerType: AnyObject.Type) -> KotlinBase.Type {
        func wrap<T>(_ cls: T.Type) -> KotlinBase.Type {
            _KotlinExistential<T>.self
        }

        func openAndWrap(_ markerType: AnyObject.Type) -> KotlinBase.Type {
            return _openExistential(markerType, do: wrap)
        }

        return openAndWrap(markerType)
    }
}