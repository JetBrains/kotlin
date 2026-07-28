declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtList<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        }
    }
    namespace foo {
        interface FunIFace {
            apply(x: string): string;
            readonly [foo.FunIFace.Symbol]: true;
        }
        namespace FunIFace {
            const Symbol: unique symbol;
        }
        interface ExportedParent {
            parentPropertyToImplement: string;
            anotherParentMethod(): kotlin.collections.KtList<string>;
            parentAsyncMethod(): Promise<string>;
            setGetterAndSetterWithJsName(_set___: string): void;
            getGetterAndSetterWithJsName(): string;
            withDefaultImplementation(): string;
            anotherDefaultImplementation(): string;
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
            readonly fooProperty: string;
            foo(): string;
            asyncFoo(): Promise<string>;
            withDefaults(value?: string): string;
            withBridge(x: T): T;
            withDefaultsAndDefaultImplementation(value?: string): string;
            suspendWithDefaultImplementation(): Promise<string>;
            genericWithDefaultImplementation<T_0>(x: T_0): string;
            delegatingToSuperDefaultImplementation(): string;
            anotherDefaultImplementation(): string;
            readonly propertyWithDefaultGetter: string;
            getT(): T;
            setTWithDefaultImpl(value: T): void;
            getTWithDefaultImpl(): T;
            readonly [foo.IFoo.Symbol]: true;
        }
        namespace IFoo {
            const Symbol: unique symbol;
            namespace DefaultImpls {
                function withDefaultsAndDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>, value?: string): string;
                function suspendWithDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): Promise<string>;
                function genericWithDefaultImplementation<T_I1 extends unknown/* kotlin.Comparable<T_I1> */, T>($this: foo.IFoo<T_I1>, x: T): string;
                function delegatingToSuperDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                function anotherDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                const propertyWithDefaultGetter: {
                    get<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                };
                function setTWithDefaultImpl<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>, value: T): void;
                function getTWithDefaultImpl<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): T;
            }
        }
        function makeFunInterfaceWithSam(): foo.FunIFace;
        function makeNoRuntimeFunInterfaceWithSam(): foo.NoRuntimeFunIface;
        function callFunInterface(f: foo.FunIFace, x: string): string;
        function callNoRuntimeFunInterface(f: foo.NoRuntimeFunIface): Array<string>;
        function callingExportedParentMethod(foo: foo.IFoo<any>): string;
        function justCallFoo(foo: foo.IFoo<any>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any>): Promise<string>;
        function justCallSuspendWithDefaultImplementation(foo: foo.IFoo<any>): Promise<string>;
        function callTypeScriptDefaultSuspend(value: foo.TypeScriptDefaultSuspend): Promise<string>;
        interface TypeScriptDefaultSuspend {
            marker(): string;
            suspendDefault(): Promise<string>;
            readonly [foo.TypeScriptDefaultSuspend.Symbol]: true;
        }
        namespace TypeScriptDefaultSuspend {
            const Symbol: unique symbol;
            namespace DefaultImpls {
                function suspendDefault($this: foo.TypeScriptDefaultSuspend): Promise<string>;
            }
        }
        function callTsAbstractSuspend(value: foo.TsSuspendDispatch): Promise<string>;
        interface TsSuspendDispatch {
            abstractSuspend(): Promise<string>;
            readonly [foo.TsSuspendDispatch.Symbol]: true;
        }
        namespace TsSuspendDispatch {
            const Symbol: unique symbol;
        }
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
        class KotlinFooImpl implements foo.IFoo<string> {
            constructor();
            get fooProperty(): string;
            get parentPropertyToImplement(): string;
            set parentPropertyToImplement(value: string);
            setGetterAndSetterWithJsName(value: string): void;
            getGetterAndSetterWithJsName(): string;
            foo(): string;
            anotherParentMethod(): kotlin.collections.KtList<string>;
            withBridge(x: string): string;
            withDefaults(value?: string): string;
            asyncFoo(): Promise<string>;
            parentAsyncMethod(): Promise<string>;
            delegatingToSuperDefaultImplementation(): string;
            getT(): string;
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
        interface Listener {
            readonly id: string;
            onStart(): string;
        }
        function beginWork(listener: foo.Listener): string;
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
            get a(): string;
            child(): string;
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
        interface ShouldBeNotImplementableWithIgnoredProperty {
            leaf(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ShouldBeNotImplementableWithIgnoredProperty": unique symbol;
            };
        }
        interface ShouldBeNotImplementableWithIgnoredFun {
            leaf(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ShouldBeNotImplementableWithIgnoredFun": unique symbol;
            };
        }
        interface ShouldBeNotImplementableWithIgnoredSuspend {
            leaf(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ShouldBeNotImplementableWithIgnoredSuspend": unique symbol;
            };
        }
        interface SuperOfSealed1 {
            sos1(): void;
            readonly [foo.SuperOfSealed1.Symbol]: true;
        }
        namespace SuperOfSealed1 {
            const Symbol: unique symbol;
        }
        interface SuperOfSealed2 {
            sos2(): void;
            readonly [foo.SuperOfSealed2.Symbol]: true;
        }
        namespace SuperOfSealed2 {
            const Symbol: unique symbol;
        }
        interface Sealed extends foo.SuperOfSealed1, foo.SuperOfSealed2 {
            readonly value: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Sealed": unique symbol;
            };
        }
        interface SealedB extends foo.Sealed, foo.FunIFace {
            readonly [foo.SealedB.Symbol]: true;
        }
        namespace SealedB {
            const Symbol: unique symbol;
        }
        interface InterfaceInheritingFromSealed extends foo.SealedB {
            readonly value2: string;
            readonly [foo.InterfaceInheritingFromSealed.Symbol]: true;
        }
        namespace InterfaceInheritingFromSealed {
            const Symbol: unique symbol;
        }
        class ClassInheritingFromSealed implements foo.SealedB {
            constructor();
            get value(): string;
            sos1(): void;
            sos2(): void;
            apply(x: string): string;
            readonly [foo.SealedB.Symbol]: true;
            readonly [foo.FunIFace.Symbol]: true;
            readonly [foo.SuperOfSealed2.Symbol]: true;
            readonly [foo.SuperOfSealed1.Symbol]: true;
            readonly __doNotUseOrImplementIt: foo.Sealed["__doNotUseOrImplementIt"];
        }
        namespace ClassInheritingFromSealed {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassInheritingFromSealed;
            }
        }
        interface SealedNonExportedImplementor {
            readonly value: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SealedNonExportedImplementor": unique symbol;
            };
        }
        class SealedNonExportedImplementorA implements foo.SealedNonExportedImplementor {
            constructor(value: string);
            get value(): string;
            copy(value?: string): foo.SealedNonExportedImplementorA;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            readonly __doNotUseOrImplementIt: foo.SealedNonExportedImplementor["__doNotUseOrImplementIt"];
        }
        namespace SealedNonExportedImplementorA {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SealedNonExportedImplementorA;
            }
        }
        interface SealedNoRuntime {
            readonly value: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SealedNoRuntime": unique symbol;
            };
        }
        class SealedNoRuntimeA implements foo.SealedNoRuntime {
            constructor(value: string);
            get value(): string;
            copy(value?: string): foo.SealedNoRuntimeA;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            readonly __doNotUseOrImplementIt: foo.SealedNoRuntime["__doNotUseOrImplementIt"];
        }
        namespace SealedNoRuntimeA {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SealedNoRuntimeA;
            }
        }
        interface SealedNoRuntimeB extends foo.SealedNoRuntime {
            readonly [foo.SealedNoRuntimeB.Symbol]: true;
        }
        namespace SealedNoRuntimeB {
            const Symbol: unique symbol;
        }
        interface InterfaceInheritingFromSealedNoRuntime extends foo.SealedNoRuntimeB {
            readonly value2: string;
            readonly [foo.InterfaceInheritingFromSealedNoRuntime.Symbol]: true;
        }
        namespace InterfaceInheritingFromSealedNoRuntime {
            const Symbol: unique symbol;
        }
        class ClassInheritingFromSealedNoRuntime implements foo.SealedNoRuntimeB {
            constructor();
            get value(): string;
            readonly [foo.SealedNoRuntimeB.Symbol]: true;
            readonly __doNotUseOrImplementIt: foo.SealedNoRuntime["__doNotUseOrImplementIt"];
        }
        namespace ClassInheritingFromSealedNoRuntime {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassInheritingFromSealedNoRuntime;
            }
        }
        interface SealedNoRuntimeWithNonExportedImplementor {
            readonly value: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SealedNoRuntimeWithNonExportedImplementor": unique symbol;
            };
        }
        class SealedNoRuntimeWithNonExportedImplementorA implements foo.SealedNoRuntimeWithNonExportedImplementor {
            constructor(value: string);
            get value(): string;
            copy(value?: string): foo.SealedNoRuntimeWithNonExportedImplementorA;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            readonly __doNotUseOrImplementIt: foo.SealedNoRuntimeWithNonExportedImplementor["__doNotUseOrImplementIt"];
        }
        namespace SealedNoRuntimeWithNonExportedImplementorA {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SealedNoRuntimeWithNonExportedImplementorA;
            }
        }
    }
}
