@_implementationOnly import KotlinBridges_overrides_across_modules
import KotlinRuntime
import KotlinRuntimeSupport
import overrides

open class Cousin: overrides.Parent {
    open override var primitiveTypeVar: Swift.Int32 {
        get {
            return Cousin_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    public override init(
        value: Swift.String
    ) {
        let __kt = __root___Cousin_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Cousin_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, value); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public final override func finalOverrideFunc() -> Swift.Void {
        return { Cousin_finalOverrideFunc(self.__externalRCRef()); return () }()
    }
    open override func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
}
public final class FinalDerived3: overrides.AbstractDerived2 {
    public override init() {
        let __kt = __root___FinalDerived3_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    public override init(
        x: Swift.Int32
    ) {
        let __kt = __root___FinalDerived3_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___FinalDerived3_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, x); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func abstractFun1() -> Swift.Void {
        return { FinalDerived3_abstractFun1(self.__externalRCRef()); return () }()
    }
}
@_cdecl("Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift")
public func Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ arg: Swift.Int32) -> Swift.Int32 {
    let _self = overrides_across_modules.Cousin.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.primitiveTypeFunc(arg: arg)
    return _result
}
