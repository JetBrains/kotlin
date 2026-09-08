@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
@_exported import KotlinCoroutineSupport
import KotlinRuntime
@_implementationOnly import KotlinBridges_KotlinxAtomicFu

public typealias locks = ExportedKotlinPackages.kotlinx.atomicfu.locks
extension ExportedKotlinPackages.kotlinx.atomicfu.locks {
    open class SynchronizedObject: KotlinRuntime.KotlinBase {
        public final func lock() -> Swift.Void {
            return { kotlinx_atomicfu_locks_SynchronizedObject_lock(self.__externalRCRef()); return () }()
        }
        public final func tryLock() -> Swift.Bool {
            return kotlinx_atomicfu_locks_SynchronizedObject_tryLock(self.__externalRCRef())
        }
        public final func unlock() -> Swift.Void {
            return { kotlinx_atomicfu_locks_SynchronizedObject_unlock(self.__externalRCRef()); return () }()
        }
        public init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject.self {
                 __kt = kotlinx_atomicfu_locks_SynchronizedObject_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
