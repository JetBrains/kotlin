declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        function makeFunInterfaceWithSam(): foo.FunIFace;
        function makeNoRuntimeFunInterfaceWithSam(): foo.NoRuntimeFunIface;
        function callFunInterface(f: foo.FunIFace, x: string): string;
        function callNoRuntimeFunInterface(f: foo.NoRuntimeFunIface): Array<string>;
        function callingExportedParentMethod(foo: foo.IFoo<any>): string;
        function justCallFoo(foo: foo.IFoo<any>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any>): Promise<string>;
        function justCallSuspendWithDefaultImplementation(foo: foo.IFoo<any>): Promise<string>;
        function callingWithDefaultsWithoutParameter(foo: foo.IFoo<any>): string;
        function callingWithDefaultsAndDefaultImplementationWithParameter(foo: foo.IFoo<any>): string;
        function callingWithDefaultsAndDefaultImplementationWithoutParameter(foo: foo.IFoo<any>): string;
        function callingWithDefaultsWithParameter(foo: foo.IFoo<any>): string;
        function callingWithBridge(foo: foo.IFoo<string>): string;
        function checkIsFooInterface(foo: any): boolean;
        function checkIsExportedParentInterface(foo: any): boolean;
        function callingWithDefaultImplementation(foo: foo.IFoo<any>): string;
        function callingAnotherWithDefaultImplementation(foo: foo.IFoo<any>): string;
        function callGenericWithDefaultImplementation(foo: foo.IFoo<any>, x: Nullable<any>): string;
        function callingDelegatingToSuperDefaultImplementation(foo: foo.IFoo<any>): string;
        interface FunIFace {
            apply(x: string): string;
            readonly [foo.FunIFace.Symbol]: true;
        }
        namespace FunIFace {
            const Symbol: unique symbol;
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
            readonly [foo.ExportedParent.Symbol]: true;
        }
        namespace ExportedParent {
            const Symbol: unique symbol;
            namespace DefaultImpls {
                function withDefaultImplementation($this: foo.ExportedParent): string;
                function anotherDefaultImplementation($this: foo.ExportedParent): string;
                const propertyWithDefaultSetter: {
                    get($this: foo.ExportedParent): string;
                    set($this: foo.ExportedParent, value: string): void;
                };
                function setDefaultGetterAndSetterWithJsName($this: foo.ExportedParent, value: string): void;
                function getDefaultGetterAndSetterWithJsName($this: foo.ExportedParent): string;
            }
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
            getT(): T;
            readonly fooProperty: string;
            readonly propertyWithDefaultGetter: string;
            setTWithDefaultImpl(value: T): void;
            getTWithDefaultImpl(): T;
            readonly [foo.IFoo.Symbol]: true;
        }
        namespace IFoo {
            const Symbol: unique symbol;
            namespace DefaultImpls {
                function withDefaultsAndDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>, value?: string): string;
                function suspendWithDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): Promise<string>;
                function genericWithDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */, T_0>($this: foo.IFoo<T>, x: T_0): string;
                function delegatingToSuperDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                function anotherDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                const propertyWithDefaultGetter: {
                    get<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                };
                function setTWithDefaultImpl<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>, value: T): void;
                function getTWithDefaultImpl<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): T;
            }
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
            getT(): string;
            get fooProperty(): string;
            get parentPropertyToImplement(): string;
            set parentPropertyToImplement(value: string);
            setGetterAndSetterWithJsName(value: string): void;
            getGetterAndSetterWithJsName(): string;
            withDefaultsAndDefaultImplementation(value?: string): string;
            suspendWithDefaultImplementation(): Promise<string>;
            genericWithDefaultImplementation<T>(x: T): string;
            anotherDefaultImplementation(): string;
            get propertyWithDefaultGetter(): string;
            setTWithDefaultImpl(value: string): void;
            getTWithDefaultImpl(): string;
            withDefaultImplementation(): string;
            get propertyWithDefaultSetter(): string;
            set propertyWithDefaultSetter(value: string);
            setDefaultGetterAndSetterWithJsName(value: string): void;
            getDefaultGetterAndSetterWithJsName(): string;
            readonly [foo.IFoo.Symbol]: true;
            readonly [foo.ExportedParent.Symbol]: true;
        }
        namespace KotlinFooImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KotlinFooImpl;
            }
        }
        interface NoRuntimeIface {
            readonly a: string;
        }
        interface NoRuntimeFunIface {
            run(): Array<string>;
        }
        interface ChildOfNoRuntime extends foo.NoRuntimeIface {
            child(): string;
        }
        class KotlinNoRuntimeImpl implements foo.NoRuntimeIface {
            constructor(a: string);
            get a(): string;
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
        }
        namespace KotlinChildNoRuntimeImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KotlinChildNoRuntimeImpl;
            }
        }
        interface NoRuntimeBase {
            base(): string;
        }
        interface MidNormal extends foo.NoRuntimeBase {
            mid(): string;
            readonly [foo.MidNormal.Symbol]: true;
        }
        namespace MidNormal {
            const Symbol: unique symbol;
        }
        interface WithSuspendOnly {
            mid(): Promise<string>;
            readonly [foo.WithSuspendOnly.Symbol]: true;
        }
        namespace WithSuspendOnly {
            const Symbol: unique symbol;
        }
        interface WithSuspendOnlyButIgnored {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.WithSuspendOnlyButIgnored": unique symbol;
            };
        }
        interface ImplementableChildOfSuspendOnlyButIgnored extends foo.WithSuspendOnlyButIgnored {
            another(): Promise<number>;
            readonly [foo.ImplementableChildOfSuspendOnlyButIgnored.Symbol]: true;
            readonly __doNotUseOrImplementIt: foo.WithSuspendOnlyButIgnored["__doNotUseOrImplementIt"];
        }
        namespace ImplementableChildOfSuspendOnlyButIgnored {
            const Symbol: unique symbol;
        }
        interface NotImplementableChildOfSuspendOnlyButIgnored extends foo.WithSuspendOnlyButIgnored {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NotImplementableChildOfSuspendOnlyButIgnored": unique symbol;
            } & foo.WithSuspendOnlyButIgnored["__doNotUseOrImplementIt"];
        }
        interface NoRuntimeLeaf extends foo.MidNormal {
            leaf(): string;
        }
        interface ShouldBeNotImplementable {
            leaf(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ShouldBeNotImplementable": unique symbol;
            };
        }
    }
}
