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

// MARK: - Variance Projection Types

public typealias KotlinCovariant<T> = (_owner: KotlinRuntime.KotlinBase, _variance: (Swift.Never) -> T)
public typealias KotlinContravariant<T> = (_owner: KotlinRuntime.KotlinBase, _variance: (T) -> Swift.Void)

@inlinable
@inline(__always)
public func _absurd<T>(_ never: Swift.Never) -> T {}

@inlinable
@inline(__always)
public func _noop<T>(_ value: T) {}

// MARK: - @usableFromInline C-Bridge Trampolines for Library Evolution Specialization

@usableFromInline internal func _bridge_Int8_box(_ v: Int8) -> UnsafeMutableRawPointer { KotlinBridgeable_Int8_box(v) }
@usableFromInline internal func _bridge_Int8_unbox(_ r: UnsafeMutableRawPointer) -> Int8 { KotlinBridgeable_Int8_unbox(r) }
@usableFromInline internal func _bridge_Int16_box(_ v: Int16) -> UnsafeMutableRawPointer { KotlinBridgeable_Int16_box(v) }
@usableFromInline internal func _bridge_Int16_unbox(_ r: UnsafeMutableRawPointer) -> Int16 { KotlinBridgeable_Int16_unbox(r) }
@usableFromInline internal func _bridge_Int32_box(_ v: Int32) -> UnsafeMutableRawPointer { KotlinBridgeable_Int32_box(v) }
@usableFromInline internal func _bridge_Int32_unbox(_ r: UnsafeMutableRawPointer) -> Int32 { KotlinBridgeable_Int32_unbox(r) }
@usableFromInline internal func _bridge_Int64_box(_ v: Int64) -> UnsafeMutableRawPointer { KotlinBridgeable_Int64_box(v) }
@usableFromInline internal func _bridge_Int64_unbox(_ r: UnsafeMutableRawPointer) -> Int64 { KotlinBridgeable_Int64_unbox(r) }
@usableFromInline internal func _bridge_UInt8_box(_ v: UInt8) -> UnsafeMutableRawPointer { KotlinBridgeable_UInt8_box(v) }
@usableFromInline internal func _bridge_UInt8_unbox(_ r: UnsafeMutableRawPointer) -> UInt8 { KotlinBridgeable_UInt8_unbox(r) }
@usableFromInline internal func _bridge_UInt16_box(_ v: UInt16) -> UnsafeMutableRawPointer { KotlinBridgeable_UInt16_box(v) }
@usableFromInline internal func _bridge_UInt16_unbox(_ r: UnsafeMutableRawPointer) -> UInt16 { KotlinBridgeable_UInt16_unbox(r) }
@usableFromInline internal func _bridge_UInt32_box(_ v: UInt32) -> UnsafeMutableRawPointer { KotlinBridgeable_UInt32_box(v) }
@usableFromInline internal func _bridge_UInt32_unbox(_ r: UnsafeMutableRawPointer) -> UInt32 { KotlinBridgeable_UInt32_unbox(r) }
@usableFromInline internal func _bridge_UInt64_box(_ v: UInt64) -> UnsafeMutableRawPointer { KotlinBridgeable_UInt64_box(v) }
@usableFromInline internal func _bridge_UInt64_unbox(_ r: UnsafeMutableRawPointer) -> UInt64 { KotlinBridgeable_UInt64_unbox(r) }
@usableFromInline internal func _bridge_Bool_box(_ v: Bool) -> UnsafeMutableRawPointer { KotlinBridgeable_Bool_box(v) }
@usableFromInline internal func _bridge_Bool_unbox(_ r: UnsafeMutableRawPointer) -> Bool { KotlinBridgeable_Bool_unbox(r) }
@usableFromInline internal func _bridge_Float_box(_ v: Float) -> UnsafeMutableRawPointer { KotlinBridgeable_Float_box(v) }
@usableFromInline internal func _bridge_Float_unbox(_ r: UnsafeMutableRawPointer) -> Float { KotlinBridgeable_Float_unbox(r) }
@usableFromInline internal func _bridge_Double_box(_ v: Double) -> UnsafeMutableRawPointer { KotlinBridgeable_Double_box(v) }
@usableFromInline internal func _bridge_Double_unbox(_ r: UnsafeMutableRawPointer) -> Double { KotlinBridgeable_Double_unbox(r) }
@usableFromInline internal func _bridge_String_box(_ v: String) -> UnsafeMutableRawPointer { KotlinBridgeable_String_box(v) }
@usableFromInline internal func _bridge_String_unbox(_ r: UnsafeMutableRawPointer) -> String { KotlinBridgeable_String_unbox(r) }
@usableFromInline internal func _bridge_Unit_box() -> UnsafeMutableRawPointer { KotlinBridgeable_Unit_box() }
@usableFromInline internal func _bridge_Array_box(_ v: [Any]) -> UnsafeMutableRawPointer { KotlinBridgeable_Array_box(v) }
@usableFromInline internal func _bridge_Array_unbox(_ r: UnsafeMutableRawPointer) -> [Any] { KotlinBridgeable_Array_unbox(r) }
@usableFromInline internal func _bridge_Set_box(_ v: Set<AnyHashable>) -> UnsafeMutableRawPointer { KotlinBridgeable_Set_box(v) }
@usableFromInline internal func _bridge_Set_unbox(_ r: UnsafeMutableRawPointer) -> Set<AnyHashable> { KotlinBridgeable_Set_unbox(r) }
@usableFromInline internal func _bridge_Dictionary_box(_ v: [AnyHashable: Any]) -> UnsafeMutableRawPointer { KotlinBridgeable_Dictionary_box(v) }
@usableFromInline internal func _bridge_Dictionary_unbox(_ r: UnsafeMutableRawPointer) -> [AnyHashable: Any] { KotlinBridgeable_Dictionary_unbox(r) }
@usableFromInline internal func _bridge_List_create(_ capacity: Int32) -> UnsafeMutableRawPointer { KotlinBridgeable_List_create(capacity) }
@usableFromInline internal func _bridge_List_add(_ listRef: UnsafeMutableRawPointer, _ elemRef: UnsafeMutableRawPointer?) { KotlinBridgeable_List_add(listRef, elemRef) }
@usableFromInline internal func _bridge_List_size(_ r: UnsafeMutableRawPointer) -> Int32 { KotlinBridgeable_List_size(r) }
@usableFromInline internal func _bridge_List_get(_ r: UnsafeMutableRawPointer, _ idx: Int32) -> UnsafeMutableRawPointer? { KotlinBridgeable_List_get(r, idx) }
@usableFromInline internal func _bridge_Set_create(_ capacity: Int32) -> UnsafeMutableRawPointer { KotlinBridgeable_Set_create(capacity) }
@usableFromInline internal func _bridge_Set_add(_ setRef: UnsafeMutableRawPointer, _ elemRef: UnsafeMutableRawPointer?) { KotlinBridgeable_Set_add(setRef, elemRef) }
@usableFromInline internal func _bridge_Set_toList(_ r: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer { KotlinBridgeable_Set_toList(r) }
@usableFromInline internal func _bridge_Map_create(_ capacity: Int32) -> UnsafeMutableRawPointer { KotlinBridgeable_Map_create(capacity) }
@usableFromInline internal func _bridge_Map_put(_ mapRef: UnsafeMutableRawPointer, _ keyRef: UnsafeMutableRawPointer?, _ valRef: UnsafeMutableRawPointer?) { KotlinBridgeable_Map_put(mapRef, keyRef, valRef) }
@usableFromInline internal func _bridge_Map_entriesList(_ r: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer { KotlinBridgeable_Map_entriesList(r) }
@usableFromInline internal func _bridge_cloneRef(_ r: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer { KotlinBridgeable_cloneRef(r) }
@usableFromInline internal func _bridge_disposeRef(_ r: UnsafeMutableRawPointer) { KotlinBridgeable_disposeRef(r) }
@usableFromInline internal func _bridge_createBridgeable(_ r: UnsafeMutableRawPointer) -> any _KotlinBridgeable {
    return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: r)
}

@inlinable
public func _disposeBridgeableRef(_ ptr: UnsafeMutableRawPointer?) {
    if let ptr = ptr {
        _bridge_disposeRef(ptr)
    }
}

@inlinable
public func _cloneBridgeableRef(_ ptr: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer {
    return _bridge_cloneRef(ptr)
}

// MARK: - _KotlinBridgeable & _KotlinCustomPointerBridgeable

public protocol _KotlinCustomPointerBridgeable: KotlinRuntime.KotlinBase {}

public protocol _KotlinOpenGenericBridgeable: _KotlinCustomPointerBridgeable {}

@usableFromInline
internal func _boxAnyHashable(_ value: any Swift.Hashable) -> Swift.AnyHashable {
    func _open<H: Swift.Hashable>(_ h: H) -> Swift.AnyHashable { Swift.AnyHashable(h) }
    return _open(value)
}

public protocol _KotlinBridgeable {
    init(__externalRCRefUnsafe: UnsafeMutableRawPointer, options: KotlinBaseConstructionOptions)
    func __externalRCRef() -> UnsafeMutableRawPointer

    static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Self
    func __toKotlinPointer() -> UnsafeMutableRawPointer?
    func __retainedToKotlinPointer() -> UnsafeMutableRawPointer?
    static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?)
}

extension _KotlinBridgeable {
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Self {
        let v = Self(__externalRCRefUnsafe: ptr!, options: .asBestFittingWrapper)
        _bridge_disposeRef(ptr!)
        return v
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        return self.__externalRCRef()
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        return self.__externalRCRef()
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
        if let ptr = ptr {
            _bridge_disposeRef(ptr)
        }
    }
}

public class _KotlinExistentialPenBox: KotlinBase {

}

public class _KotlinExistential<Wrapped>: _KotlinExistentialPenBox, _KotlinCustomPointerBridgeable {
}

extension KotlinBase: _KotlinBridgeable {
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Self {
        if Self.self is any _KotlinOpenGenericBridgeable.Type {
            if let existing = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: _bridge_cloneRef(ptr!)) as? Self {
                _bridge_disposeRef(ptr!)
                return existing
            }
            return Self(__externalRCRefUnsafe: ptr!, options: .asExistentialWrapper)
        }
        if Self.self is any _KotlinCustomPointerBridgeable.Type {
            return Self(__externalRCRefUnsafe: ptr!, options: .asExistentialWrapper)
        }
        return Self.__createClassWrapper(externalRCRef: ptr!)
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        return self.__externalRCRef()
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        return _bridge_cloneRef(self.__externalRCRef())
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
    }
}

// MARK: - Optional, Never, KotlinUnit, KotlinAny _KotlinBridgeable conformances

extension Swift.Optional: KotlinRuntimeSupport._KotlinBridgeable where Wrapped: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = .some(Wrapped(__externalRCRefUnsafe: ref, options: options))
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        switch self {
        case .some(let val):
            return val.__externalRCRef()
        case .none:
            preconditionFailure("Cannot obtain non-null __externalRCRef from nil Optional")
        }
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Wrapped? {
        guard let ptr = ptr else { return nil }
        return .some(Wrapped.__fromKotlinPointer(ptr))
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        switch self {
        case .some(let val):
            return val.__toKotlinPointer()
        case .none:
            return nil
        }
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        switch self {
        case .some(let val):
            return val.__retainedToKotlinPointer()
        case .none:
            return nil
        }
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
        Wrapped.__disposeTemporaryRCRef(ptr)
    }
}

extension Swift.Never: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        fatalError("Never cannot be instantiated")
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        switch self {}
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Never {
        fatalError("Never cannot be instantiated")
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        switch self {}
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        switch self {}
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
    }
}

@frozen
public struct KotlinUnit: KotlinRuntimeSupport._KotlinBridgeable, Equatable, Hashable {
    public static let shared = KotlinUnit()

    @inlinable
    public init() {}

    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Unit_box()
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> KotlinUnit {
        if let ptr = ptr { _bridge_disposeRef(ptr) }
        return KotlinUnit()
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        return _bridge_Unit_box()
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        return _bridge_Unit_box()
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
        if let ptr = ptr { _bridge_disposeRef(ptr) }
    }
}

@frozen
public struct KotlinAny: KotlinRuntimeSupport._KotlinBridgeable {
    public let value: any KotlinRuntimeSupport._KotlinBridgeable

    @inlinable
    public init(_ value: any KotlinRuntimeSupport._KotlinBridgeable) {
        self.value = value
    }

    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self.value = _bridge_createBridgeable(_bridge_cloneRef(ref))
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return self.value.__externalRCRef()
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> KotlinAny {
        return KotlinAny(_bridge_createBridgeable(ptr!))
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        return self.value.__retainedToKotlinPointer()
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        return self.value.__retainedToKotlinPointer()
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
        if let ptr = ptr { _bridge_disposeRef(ptr) }
    }
}

extension Swift.AnyHashable: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        let v = _bridge_createBridgeable(_bridge_cloneRef(ref))
        self = _boxAnyHashable(v as! any Swift.Hashable)
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return (self.base as! any KotlinRuntimeSupport._KotlinBridgeable).__externalRCRef()
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.AnyHashable {
        let v = _bridge_createBridgeable(ptr!)
        return _boxAnyHashable(v as! any Swift.Hashable)
    }

    @inlinable
    public func __toKotlinPointer() -> UnsafeMutableRawPointer? {
        return (self.base as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
    }

    @inlinable
    public func __retainedToKotlinPointer() -> UnsafeMutableRawPointer? {
        return (self.base as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
    }

    @inlinable
    public static func __disposeTemporaryRCRef(_ ptr: UnsafeMutableRawPointer?) {
        if let ptr = ptr { _bridge_disposeRef(ptr) }
    }
}

// MARK: - _KotlinBridgeable conformances for primitive types

extension Swift.Int8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Int8_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Int8_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Int8 {
        let v = _bridge_Int8_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Int16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Int16_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Int16_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Int16 {
        let v = _bridge_Int16_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Int32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Int32_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Int32_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Int32 {
        let v = _bridge_Int32_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Int64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Int64_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Int64_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Int64 {
        let v = _bridge_Int64_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.UInt8: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_UInt8_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_UInt8_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.UInt8 {
        let v = _bridge_UInt8_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.UInt16: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_UInt16_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_UInt16_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.UInt16 {
        let v = _bridge_UInt16_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.UInt32: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_UInt32_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_UInt32_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.UInt32 {
        let v = _bridge_UInt32_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.UInt64: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_UInt64_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_UInt64_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.UInt64 {
        let v = _bridge_UInt64_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Bool: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Bool_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Bool_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Bool {
        let v = _bridge_Bool_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Float: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Float_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Float_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Float {
        let v = _bridge_Float_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Double: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_Double_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_Double_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.Double {
        let v = _bridge_Double_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

// MARK: - _KotlinBridgeable conformance for String

extension Swift.String: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        self = _bridge_String_unbox(ref)
    }
    @inlinable
    public func __externalRCRef() -> UnsafeMutableRawPointer {
        return _bridge_String_box(self)
    }
    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Swift.String {
        let v = _bridge_String_unbox(ptr!)
        _bridge_disposeRef(ptr!)
        return v
    }
}

// MARK: - _KotlinBridgeable conformances for collection types

extension Swift.Array: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        if let elemBridge = Element.self as? any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let count = _bridge_List_size(ref)
            var result: [Element] = []
            result.reserveCapacity(Int(count))
            for i in 0..<count {
                let elemPtr = _bridge_List_get(ref, i)
                result.append(elemBridge.__fromKotlinPointer(elemPtr) as! Element)
            }
            self = result
        } else {
            self = _bridge_Array_unbox(ref) as! [Element]
        }
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        if Element.self is any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let listRef = _bridge_List_create(Int32(self.count))
            for elem in self {
                let elemPtr = (elem as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
                _bridge_List_add(listRef, elemPtr)
            }
            return listRef
        } else {
            return _bridge_Array_box(self.map { $0 as Any })
        }
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> [Element] {
        let v = Self(__externalRCRefUnsafe: ptr!, options: .asBestFittingWrapper)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Set: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        if let elemBridge = Element.self as? any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let listRef = _bridge_Set_toList(ref)
            defer { _bridge_disposeRef(listRef) }
            let count = _bridge_List_size(listRef)
            var result = Set<Element>(minimumCapacity: Int(count))
            for i in 0..<count {
                let elemPtr = _bridge_List_get(listRef, i)
                result.insert(elemBridge.__fromKotlinPointer(elemPtr) as! Element)
            }
            self = result
        } else {
            self = _bridge_Set_unbox(ref) as! Set<Element>
        }
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        if Element.self is any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let setRef = _bridge_Set_create(Int32(self.count))
            for elem in self {
                let elemPtr = (elem as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
                _bridge_Set_add(setRef, elemPtr)
            }
            return setRef
        } else {
            return _bridge_Set_box(self)
        }
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> Set<Element> {
        let v = Self(__externalRCRefUnsafe: ptr!, options: .asBestFittingWrapper)
        _bridge_disposeRef(ptr!)
        return v
    }
}

extension Swift.Dictionary: KotlinRuntimeSupport._KotlinBridgeable {
    public init(__externalRCRefUnsafe ref: UnsafeMutableRawPointer, options: KotlinRuntime.KotlinBaseConstructionOptions) {
        if let keyBridge = Key.self as? any KotlinRuntimeSupport._KotlinBridgeable.Type,
           let valBridge = Value.self as? any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let flatRef = _bridge_Map_entriesList(ref)
            defer { _bridge_disposeRef(flatRef) }
            let count = _bridge_List_size(flatRef)
            var result: [Key: Value] = [:]
            result.reserveCapacity(Int(count / 2))
            var i: Int32 = 0
            while i < count {
                let kPtr = _bridge_List_get(flatRef, i)
                let vPtr = _bridge_List_get(flatRef, i + 1)
                let k = keyBridge.__fromKotlinPointer(kPtr) as! Key
                let v = valBridge.__fromKotlinPointer(vPtr) as! Value
                result[k] = v
                i += 2
            }
            self = result
        } else {
            self = _bridge_Dictionary_unbox(ref) as! [Key: Value]
        }
    }

    public func __externalRCRef() -> UnsafeMutableRawPointer {
        if Key.self is any KotlinRuntimeSupport._KotlinBridgeable.Type,
           Value.self is any KotlinRuntimeSupport._KotlinBridgeable.Type {
            let mapRef = _bridge_Map_create(Int32(self.count))
            for (k, v) in self {
                let kPtr = (k as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
                let vPtr = (v as! any KotlinRuntimeSupport._KotlinBridgeable).__retainedToKotlinPointer()
                _bridge_Map_put(mapRef, kPtr, vPtr)
            }
            return mapRef
        } else {
            return _bridge_Dictionary_box(self)
        }
    }

    @inlinable
    public static func __fromKotlinPointer(_ ptr: UnsafeMutableRawPointer?) -> [Key: Value] {
        let v = Self(__externalRCRefUnsafe: ptr!, options: .asBestFittingWrapper)
        _bridge_disposeRef(ptr!)
        return v
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
        case 16: let v = KotlinBridgeable_Char_unbox(ref);                               KotlinBridgeable_disposeRef(ref); return v
        default: return __createProtocolWrapper(externalRCRef: ref, conformsTo: conformsTo)
        }
    }

    package static func __createProtocolWrapper<T>(externalRCRef ref: UnsafeMutableRawPointer, conformsTo type: T.Type) -> KotlinBase {
        return __createProtocolWrapper(externalRCRef: ref, conformsTo: { wrapperClass in wrapperClass is T })
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

// MARK: - Link anchor

/// Anchor that keeps this compilation unit in the linked image. Without it this module gets stripped.
/// Declared `package` so the symbol stays external: an internal one is hidden in a Release build
/// (Debug gets `-enable-testing`), and SwiftPM's `ld -r` merge then makes it local, so the Kotlin
/// binary's reference to it does not link (KT-90738).
@_cdecl("KotlinRuntimeSupport_linkAnchor")
package func _kotlinRuntimeSupportLinkAnchor() {}
