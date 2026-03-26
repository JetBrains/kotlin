@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_Atomicfu
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.atomicfu.locks {
    open class SynchronizedObject: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject ") }
            let __kt = kotlinx_atomicfu_locks_SynchronizedObject_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public final func lock() -> Swift.Void {
            return { kotlinx_atomicfu_locks_SynchronizedObject_lock(self.__externalRCRef()); return () }()
        }
        public final func tryLock() -> Swift.Bool {
            return kotlinx_atomicfu_locks_SynchronizedObject_tryLock(self.__externalRCRef())
        }
        public final func unlock() -> Swift.Void {
            return { kotlinx_atomicfu_locks_SynchronizedObject_unlock(self.__externalRCRef()); return () }()
        }
    }
}
