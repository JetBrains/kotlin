declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        function makeFunInterfaceWithSam(): foo.FunIFace;
        function makeNoRuntimeFunInterfaceWithSam(): foo.NoRuntimeFunIface;
        function callFunInterface(f: foo.FunIFace, x: string): string;
        function callNoRuntimeFunInterface(f: foo.NoRuntimeFunIface): Array<string>;
        function callingExportedParentMethod(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallFoo(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallSuspendWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function callingWithDefaultsWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsAndDefaultImplementationWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsAndDefaultImplementationWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithBridge(foo: foo.IFoo<string>): string;
        function checkIsFooInterface(foo: any): boolean;
        function checkIsExportedParentInterface(foo: any): boolean;
        function callingWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingAnotherWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callGenericWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>, x: Nullable<any>): string;
        function callingDelegatingToSuperDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): string;
        interface FunIFace {
            apply(x: string): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.FunIFace": unique symbol;
            };
        }
        interface ExportedParent {
            anotherParentMethod(): any/* kotlin.collections.List<string> */;
            parentAsyncMethod(): Promise<string>;
            withDefaultImplementation(): string;
            anotherDefaultImplementation(): string;
            parentPropertyToImplement: string;
            setGetterAndSetterWithJsName(value: string): void;
            getGetterAndSetterWithJsName(): string;
            propertyWithDefaultSetter: string;
            setDefaultGetterAndSetterWithJsName(value: string): void;
            getDefaultGetterAndSetterWithJsName(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedParent": unique symbol;
            };
        }
        interface IFoo<T extends unknown/* kotlin.Comparable<T> */> extends foo.ExportedParent {
            foo(): string;
            asyncFoo(): Promise<string>;
            withDefaults(value?: string): string;
            withBridge(x: T): T;
            withDefaultsAndDefaultImplementation(value?: string): string;
            suspendWithDefaultImplementation(): Promise<string>;
            genericWithDefaultImplementation<T_0>(x: T_0): string;
            delegatingToSuperDefaultImplementation(): string;
            anotherDefaultImplementation(): string;
            readonly fooProperty: string;
            readonly propertyWithDefaultGetter: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IFoo": unique symbol;
            } & foo.ExportedParent["__doNotUseOrImplementIt"];
        }
        class KotlinFooImpl implements foo.IFoo<string> {
            constructor();
            foo(): string;
            anotherParentMethod(): any/* kotlin.collections.List<string> */;
            withBridge(x: string): string;
            withDefaults(value?: string): string;
            asyncFoo(): Promise<string>;
            parentAsyncMethod(): Promise<string>;
            delegatingToSuperDefaultImplementation(): string;
            get fooProperty(): string;
            get parentPropertyToImplement(): string;
            set parentPropertyToImplement(value: string);
            get getterAndSetterWithJsName(): string;
            set getterAndSetterWithJsName(value: string);
            readonly __doNotUseOrImplementIt: foo.IFoo<string>["__doNotUseOrImplementIt"];
        }
        namespace KotlinFooImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KotlinFooImpl;
            }
        }
        interface NoRuntimeIface {
            readonly a: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NoRuntimeIface": unique symbol;
            };
        }
        interface NoRuntimeFunIface {
            run(): Array<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NoRuntimeFunIface": unique symbol;
            };
        }
        interface ChildOfNoRuntime extends foo.NoRuntimeIface {
            child(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ChildOfNoRuntime": unique symbol;
            } & foo.NoRuntimeIface["__doNotUseOrImplementIt"];
        }
        class KotlinNoRuntimeImpl implements foo.NoRuntimeIface {
            constructor(a: string);
            get a(): string;
            readonly __doNotUseOrImplementIt: foo.NoRuntimeIface["__doNotUseOrImplementIt"];
        }
        namespace KotlinNoRuntimeImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KotlinNoRuntimeImpl;
            }
        }
        class KotlinChildNoRuntimeImpl implements foo.ChildOfNoRuntime {
            constructor(a: string);
            child(): string;
            get a(): string;
            readonly __doNotUseOrImplementIt: foo.ChildOfNoRuntime["__doNotUseOrImplementIt"];
        }
        namespace KotlinChildNoRuntimeImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KotlinChildNoRuntimeImpl;
            }
        }
        interface NoRuntimeBase {
            base(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NoRuntimeBase": unique symbol;
            };
        }
        interface MidNormal extends foo.NoRuntimeBase {
            mid(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.MidNormal": unique symbol;
            } & foo.NoRuntimeBase["__doNotUseOrImplementIt"];
        }
        interface NoRuntimeLeaf extends foo.MidNormal {
            leaf(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NoRuntimeLeaf": unique symbol;
            } & foo.MidNormal["__doNotUseOrImplementIt"];
        }
    }
}
