@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dependency
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.dependency {
    open class DependencyBase: KotlinRuntime.KotlinBase {
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.dependency.DependencyBase.self {
                 __kt = dependency_DependencyBase_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { dependency_DependencyBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func checked() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.dependency.DependencyBase.self {
                return dependency_DependencyBase_checked(self.__externalRCRef())
            } else {
                return dependency_DependencyBase_checked_direct(self.__externalRCRef())
            }
        }
        @discardableResult
        open func ignorable() -> Swift.Int32 {
            if Self.self == ExportedKotlinPackages.dependency.DependencyBase.self {
                return dependency_DependencyBase_ignorable(self.__externalRCRef())
            } else {
                return dependency_DependencyBase_ignorable_direct(self.__externalRCRef())
            }
        }
    }
    public static func dependencyFunction() -> Swift.Int32 {
        return dependency_dependencyFunction()
    }
    @discardableResult
    public static func ignorableDependencyFunction() -> Swift.Int32 {
        return dependency_ignorableDependencyFunction()
    }
}
@_cdecl("dependency_DependencyBase_checked__reverse_swift")
package func dependency_DependencyBase_checked__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.dependency.DependencyBase.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.checked()
    return _result
}

@_cdecl("dependency_DependencyBase_ignorable__reverse_swift")
package func dependency_DependencyBase_ignorable__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = ExportedKotlinPackages.dependency.DependencyBase.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Int32 = _self.ignorable()
    return _result
}
