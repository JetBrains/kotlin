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
    open class BooleanIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Bool
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.BooleanIterator.self, "ExportedKotlinPackages.kotlin.collections.BooleanIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_BooleanIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func next() -> Swift.Bool {
            return kotlin_collections_BooleanIterator_next(self.__externalRCRef())
        }
        open func nextBoolean() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlin.collections.BooleanIterator.self {
                return kotlin_collections_BooleanIterator_nextBoolean(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.BooleanIterator.nextBoolean': a Swift subclass must override it and must not call super.")
            }
        }
    }
    open class IntIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator {
        public typealias T = Swift.Int32
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlin.collections.IntIterator.self, "ExportedKotlinPackages.kotlin.collections.IntIterator is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func next() -> Swift.Int32 {
            return kotlin_collections_IntIterator_next(self.__externalRCRef())
        }
        open func nextInt() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.kotlin.collections.IntIterator.self {
                return kotlin_collections_IntIterator_nextInt(self.__externalRCRef())
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlin.collections.IntIterator.nextInt': a Swift subclass must override it and must not call super.")
            }
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public final class BooleanArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_BooleanArray_size_get(self.__externalRCRef())
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
            `init`: @escaping (Swift.Int32) -> Swift.Bool
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Bool {
            return kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Bool
        ) -> Swift.Void {
            return { kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.BooleanIterator {
            return ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: kotlin_BooleanArray_iterator(self.__externalRCRef()))
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Bool {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    public final class IntArray: KotlinRuntime.KotlinBase {
        public var size: Swift.Int32 {
            get {
                return kotlin_IntArray_size_get(self.__externalRCRef())
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
            `init`: @escaping (Swift.Int32) -> Swift.Int32
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> Swift.Int32 {
            return kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func _set(
            index: Swift.Int32,
            value: Swift.Int32
        ) -> Swift.Void {
            return { kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), index, value); return () }()
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.IntIterator {
            return ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: kotlin_IntArray_iterator(self.__externalRCRef()))
        }
        public subscript(
            index: Swift.Int32
        ) -> Swift.Int32 {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
}
@_cdecl("kotlin_collections_BooleanIterator_nextBoolean__reverse_swift")
package func kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlin.collections.BooleanIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.nextBoolean()
    return _result
}

@_cdecl("kotlin_collections_IntIterator_nextInt__reverse_swift")
package func kotlin_collections_IntIterator_nextInt__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.kotlin.collections.IntIterator.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.nextInt()
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
