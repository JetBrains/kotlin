@_implementationOnly import KotlinBridges_overrides_across_modules
import overrides

open class Cousin : overrides.Parent {
    open override var primitiveTypeVar: Swift.Int32 {
        get {
            return Cousin_primitiveTypeVar_get(self.__externalRCRef())
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public override init(
        value: Swift.String
    ) {
        let __kt = __root___Cousin_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Cousin_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt, value)
    }
    public final override func finalOverrideFunc() -> Swift.Void {
        return Cousin_finalOverrideFunc(self.__externalRCRef())
    }
    open override func primitiveTypeFunc(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Cousin_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
}
