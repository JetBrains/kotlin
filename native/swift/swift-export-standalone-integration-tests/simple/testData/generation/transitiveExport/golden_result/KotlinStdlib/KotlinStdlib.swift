@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_Iterator_next(self.__externalRCRef()))
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Iterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func hasNext() -> Swift.Bool
        func next() -> T
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterator)
    public protocol _Iterator {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnyIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class ByteIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int8
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.ByteIterator.self, "ExportedKotlinPackages.kotlin.collections.ByteIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_ByteIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func next() -> Swift.Int8 {
            return kotlin_collections_ByteIterator_next(self.__externalRCRef())
        }
        open func nextByte() -> Swift.Int8 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.ByteIterator.self {
                return kotlin_collections_ByteIterator_nextByte(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.ByteIterator.nextByte': a Swift subclass must override it and must not call super.")
            }
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public final class Array<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var size: Swift.Int32 {
            get {
                return _cbridge_kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> T
        ) {
            fatalError()
        }
        @inlinable
        public func _get(
            index: Swift.Int32
        ) -> T {
            return T.__fromKotlinPointer(_cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
        }
        @inlinable
        public func _set(
            index: Swift.Int32,
            value: T
        ) -> Swift.Void {
            return { _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(self.__externalRCRef(), index, value.__retainedToKotlinPointer()); return () }()
        }
        @inlinable
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
            return { let _ptr = _cbridge_kotlin_Array_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
        @inlinable
        public subscript(
            index: Swift.Int32
        ) -> T {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class ByteArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
            }
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int8
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return { kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ByteIterator {
            return ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: kotlin_ByteArray_iterator(self.__externalRCRef()))
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int8 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
}
@_cdecl("kotlin_collections_ByteIterator_nextByte__reverse_swift")
package func kotlin_collections_ByteIterator_nextByte__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int8 {
    let _self = ExportedKotlinPackages.kotlin.collections.ByteIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int8 = _self.nextByte()
    return _result
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Array_get__TypesOfArguments__Swift_Int32__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_iterator(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Array_iterator(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    return kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_size_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_Array_size_get(_p0)
}
