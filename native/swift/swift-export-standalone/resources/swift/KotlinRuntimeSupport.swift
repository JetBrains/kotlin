@_implementationOnly import KotlinRuntimeSupportBridge
import Foundation
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

package func kotlinThrowableRCRef(for error: any Error) -> UnsafeMutableRawPointer {
    if let kotlinBase = error as? KotlinRuntime.KotlinBase, let throwableRef = SwiftError_retainedThrowableRef(kotlinBase.__externalRCRef()) {
        return throwableRef
    }

    if let kotlinError = error as? KotlinError,
       let throwableRef = SwiftError_retainedThrowableRef(kotlinError.wrapped.__externalRCRef()) {
        return throwableRef
    }
    let errorObject = error as AnyObject
    return SwiftError_create(Unmanaged.passUnretained(errorObject).toOpaque())
}

package func raiseKotlinError(_ outError: UnsafeMutableRawPointer?) throws {
    guard let outError = outError else { return }
    if let boxedError = SwiftError_unwrapBoxOrNull(outError) {
        let swiftError = Unmanaged<AnyObject>.fromOpaque(boxedError).takeUnretainedValue() as! any Error
        KotlinBridgeable_disposeRef(outError)
        throw swiftError
    }
    let wrapper = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: outError)
    throw (wrapper as? any Error) ?? KotlinError(wrapped: wrapper)
}

package func swiftError(fromKotlinThrowable wrapper: KotlinRuntime.KotlinBase) -> any Error {
    if let boxedError = SwiftError_unwrapBoxOrNull(wrapper.__externalRCRef()) {
        return Unmanaged<AnyObject>.fromOpaque(boxedError).takeUnretainedValue() as! any Error
    }
    return (wrapper as? any Error) ?? KotlinError(wrapped: wrapper)
}

public protocol SealedType {
    associatedtype T
    var value: T { get }
}

public protocol _KotlinBridgeable {
    init(__externalRCRefUnsafe: UnsafeMutableRawPointer, options: KotlinBaseConstructionOptions)
    func __externalRCRef() -> UnsafeMutableRawPointer
    func __kotlinBridgeableRef() -> KotlinBridgeableRef
}

public class _KotlinExistentialPenBox: KotlinBase {

}

public class _KotlinExistential<Wrapped>: _KotlinExistentialPenBox {

}

extension KotlinBase : _KotlinBridgeable {
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(ptrValue: KotlinBridgeable_Any_toKotlin(self))
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeObject)
    }
}

// MARK: - _KotlinBridgeable conformances for primitive types

extension Swift.Int8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int8_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Int8_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(int8Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeInt8)
    }
}

extension Swift.Int16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int16_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Int16_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(int16Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeInt16)
    }
}

extension Swift.Int32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int32_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Int32_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(int32Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeInt32)
    }
}

extension Swift.Int64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Int64_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Int64_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(int64Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeInt64)
    }
}

extension Swift.UInt8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt8_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_UInt8_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(uint8Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeUInt8)
    }
}

extension Swift.UInt16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt16_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_UInt16_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(uint16Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeUInt16)
    }
}

extension Swift.UInt32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt32_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_UInt32_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(uint32Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeUInt32)
    }
}

extension Swift.UInt64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_UInt64_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_UInt64_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(uint64Value: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeUInt64)
    }
}

extension Swift.Bool: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Bool_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Bool_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(boolValue: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeBool)
    }
}

extension Swift.Float: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Float_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Float_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(floatValue: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeFloat)
    }
}

extension Swift.Double: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Double_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Double_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(doubleValue: self)
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeDouble)
    }
}

// MARK: - _KotlinBridgeable conformance for String

extension Swift.String: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_String_unbox(ref)
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_String_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        let value = KotlinBridgeableValue(ptrValue: KotlinBridgeable_String_toKotlin(self))
        return KotlinBridgeableRef(value: value, type: KotlinBridgeableTypeString)
    }
}

// MARK: - _KotlinBridgeable conformances for collection types

extension Swift.Array: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Array_unbox(ref) as! [Element]
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Array_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        fatalError("impl __kotlinBridgeableRef for Swift collection structs")
    }
}

extension Swift.Set: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Set_unbox(ref) as! Set<Element>
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Set_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        fatalError("impl __kotlinBridgeableRef for Swift collection structs")
    }
}

extension Swift.Dictionary: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = KotlinBridgeable_Dictionary_unbox(ref) as! [Key: Value]
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return KotlinBridgeable_Dictionary_box(self)
    }
    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        fatalError("impl __kotlinBridgeableRef for Swift collection structs")
    }
}

extension Swift.Optional: KotlinRuntimeSupport._KotlinBridgeable where Wrapped == any KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        fatalError()
    }
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        fatalError()
    }

    public func __kotlinBridgeableRef() -> KotlinBridgeableRef {
        guard let value = self else {
            return KotlinBridgeableRef(value: KotlinBridgeableValue(boolValue: false), type: KotlinBridgeableTypeNil)
        }
        return value.__kotlinBridgeableRef()
    }
}

// MARK: - __createBridgeable: unwraps bridgeable types from externalRCRef

package func _kotlinAllocInstanceForSwiftSubclass(_ metaobject: Swift.AnyClass) -> Swift.UnsafeMutableRawPointer {
    return Kotlin_SwiftExport_allocInstanceForSwiftSubclass(metaobject)
}

extension KotlinBase {
    package static func __createBridgeable<T>(externalRCRef ref: UnsafeMutableRawPointer, conformsTo type: T.Type) -> any _KotlinBridgeable {
        __createBridgeable(externalRCRef: ref, conformsTo: { wrapperClass in wrapperClass is T })
    }

    package static func __createBridgeable(externalRCRef ref: UnsafeMutableRawPointer, conformsTo: ((AnyClass?) -> Bool)? = nil) -> any _KotlinBridgeable {
        let tag = KotlinBridgeable_getTypeTag(ref)
        switch tag {
        case 1:  let v = KotlinBridgeable_String_unbox(ref);                             KotlinBridgeable_disposeRef(ref); return v
        case 2:  let v = KotlinBridgeable_Int8_unbox(ref);                               KotlinBridgeable_disposeRef(ref); return v
        case 3:  let v = KotlinBridgeable_Int16_unbox(ref);                              KotlinBridgeable_disposeRef(ref); return v
        case 4:  let v = KotlinBridgeable_Int32_unbox(ref);                              KotlinBridgeable_disposeRef(ref); return v
        case 5:  let v = KotlinBridgeable_Int64_unbox(ref);                              KotlinBridgeable_disposeRef(ref); return v
        case 6:  let v = KotlinBridgeable_UInt8_unbox(ref);                              KotlinBridgeable_disposeRef(ref); return v
        case 7:  let v = KotlinBridgeable_UInt16_unbox(ref);                             KotlinBridgeable_disposeRef(ref); return v
        case 8:  let v = KotlinBridgeable_UInt32_unbox(ref);                             KotlinBridgeable_disposeRef(ref); return v
        case 9:  let v = KotlinBridgeable_UInt64_unbox(ref);                             KotlinBridgeable_disposeRef(ref); return v
        case 10: let v = KotlinBridgeable_Bool_unbox(ref);                               KotlinBridgeable_disposeRef(ref); return v
        case 11: let v = KotlinBridgeable_Float_unbox(ref);                              KotlinBridgeable_disposeRef(ref); return v
        case 12: let v = KotlinBridgeable_Double_unbox(ref);                             KotlinBridgeable_disposeRef(ref); return v
        case 13: let v: [Any] = KotlinBridgeable_Array_unbox(ref) as! [Any];             KotlinBridgeable_disposeRef(ref); return v
        case 14: let v = KotlinBridgeable_Set_unbox(ref) as! Set<AnyHashable>;           KotlinBridgeable_disposeRef(ref); return v
        case 15: let v = KotlinBridgeable_Dictionary_unbox(ref) as! [AnyHashable: Any];  KotlinBridgeable_disposeRef(ref); return v
        default: return __createProtocolWrapper(externalRCRef: ref, conformsTo: conformsTo)
        }
    }

    package static func __createProtocolWrapper<T>(externalRCRef ref: UnsafeMutableRawPointer, conformsTo type: T.Type) -> KotlinBase {
        return __createProtocolWrapper(externalRCRef: ref, conformsTo: { wrapperClass in wrapperClass is T })
    }

    package static func __createBridgeable(ref: KotlinBridgeableRef) -> (any _KotlinBridgeable)? {
        switch ref.type {
        case KotlinBridgeableTypeNil: return nil
        case KotlinBridgeableTypeVoid: fatalError("Make Void conform to _KotlinBridgeable")
        case KotlinBridgeableTypeBool: return ref.value.boolValue
        case KotlinBridgeableTypeInt8: return ref.value.int8Value
        case KotlinBridgeableTypeInt16: return ref.value.int16Value
        case KotlinBridgeableTypeInt32: return ref.value.int32Value
        case KotlinBridgeableTypeInt64: return ref.value.int64Value
        case KotlinBridgeableTypeUInt8: return ref.value.uint8Value
        case KotlinBridgeableTypeUInt16: return ref.value.uint16Value
        case KotlinBridgeableTypeUInt32: return ref.value.uint32Value
        case KotlinBridgeableTypeUInt64: return ref.value.uint64Value
        case KotlinBridgeableTypeDouble: return ref.value.doubleValue
        case KotlinBridgeableTypeFloat: return ref.value.floatValue
        case KotlinBridgeableTypeObject: return KotlinBridgeable_Any_fromKotlin(ref.value.ptrValue)
        case KotlinBridgeableTypeString: return KotlinBridgeable_String_fromKotlin(ref.value.ptrValue)
        default: fatalError("Unsupported KotlinBridgeableType: \(ref.type)")
        }
    }
}

package func KotlinBridgeable_Void_toKotlin(_ value: Void) -> Bool {
    return true
}

package func KotlinBridgeable_Void_fromKotlin(_ value: Bool) -> Void {
    return ()
}

package func KotlinBridgeable_OptVoid_toKotlin(_ value: Void?) -> Bool {
    return value != nil
}

package func KotlinBridgeable_OptVoid_fromKotlin(_ value: Bool) -> Void? {
    return value ? () : nil
}

package func KotlinBridgeable_Any_toKotlin(_ value: KotlinBase) -> UnsafeMutableRawPointer {
    return value.__externalRCRef()
}

package func KotlinBridgeable_Any_fromKotlin(_ value: UnsafeMutableRawPointer) -> KotlinBase {
    // TODO: Conforms to
    return KotlinBase.__createProtocolWrapper(externalRCRef: value, conformsTo: nil)
}

package func KotlinBridgeable_OptAny_toKotlin(_ value: KotlinBase?) -> UnsafeMutableRawPointer? {
    return value.map { KotlinBridgeable_Any_toKotlin($0) } ?? nil
}

package func KotlinBridgeable_OptAny_fromKotlin(_ value: UnsafeMutableRawPointer?) -> KotlinBase? {
    guard let value else { return nil }
    return KotlinBridgeable_Any_fromKotlin(value)
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

// MARK: - Link anchor

/// Anchor that keeps this compilation unit in the linked image. Without it this module gets stripped.
/// Declared `package` so the symbol stays external: an internal one is hidden in a Release build
/// (Debug gets `-enable-testing`), and SwiftPM's `ld -r` merge then makes it local, so the Kotlin
/// binary's reference to it does not link (KT-90738).
@_cdecl("KotlinRuntimeSupport_linkAnchor")
package func _kotlinRuntimeSupportLinkAnchor() {}
