declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtList<E> /* extends kotlin.collections.Collection<E> */ {
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
            }
        }
        function makeFunInterfaceWithSam(): foo.FunIFace;
        function callFunInterface(f: foo.FunIFace, x: string): string;
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
            withDefaultsAndDefaultImplementation(value?: string): string;
            suspendWithDefaultImplementation(): Promise<string>;
            genericWithDefaultImplementation<T>(x: T): string;
            anotherDefaultImplementation(): string;
            get propertyWithDefaultGetter(): string;
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
    }
}
