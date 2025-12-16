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
        interface ExportedParent {
            anotherParentMethod(): kotlin.collections.KtList<string>;
            parentAsyncMethod(): Promise<string>;
            withDefaultImplementation(): string;
            propertyWithDefaultSetter: string;
            setGetterAndSetterWithJsName(value: string): void;
            getGetterAndSetterWithJsName(): string;
            readonly [foo.ExportedParent.Symbol]: true;
        }
        namespace ExportedParent {
            const Symbol: unique symbol;
            namespace defaults {
                function withDefaultImplementation($this: foo.ExportedParent): string;
                const propertyWithDefaultSetter: {
                    get($this: foo.ExportedParent): string;
                    set($this: foo.ExportedParent, value: string): void;
                };
                function setGetterAndSetterWithJsName($this: foo.ExportedParent, value: string): void;
                function getGetterAndSetterWithJsName($this: foo.ExportedParent): string;
            }
        }
        interface IFoo<T extends unknown/* kotlin.Comparable<T> */> extends foo.ExportedParent {
            foo(): string;
            asyncFoo(): Promise<string>;
            withDefaults(value?: string): string;
            withBridge(x: T): T;
            suspendWithDefaultImplementation(): Promise<string>;
            readonly propertyWithDefaultGetter: string;
            readonly [foo.IFoo.Symbol]: true;
        }
        namespace IFoo {
            const Symbol: unique symbol;
            namespace defaults {
                function suspendWithDefaultImplementation<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): Promise<string>;
                const propertyWithDefaultGetter: {
                    get<T extends unknown/* kotlin.Comparable<T> */>($this: foo.IFoo<T>): string;
                };
            }
        }
        function callingExportedParentMethod(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallFoo(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function callingWithDefaultsWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithBridge(foo: foo.IFoo<string>): string;
        function checkIsInterface(foo: any): boolean;
        function callingWithDefaultImplementations(foo: foo.IFoo<any /*UnknownType **/>): string;
        class KotlinFooImpl implements foo.IFoo<string> {
            constructor();
            foo(): string;
            anotherParentMethod(): kotlin.collections.KtList<string>;
            withBridge(x: string): string;
            withDefaults(value?: string): string;
            asyncFoo(): Promise<string>;
            parentAsyncMethod(): Promise<string>;
            suspendWithDefaultImplementation(): Promise<string>;
            get propertyWithDefaultGetter(): string;
            withDefaultImplementation(): string;
            get propertyWithDefaultSetter(): string;
            set propertyWithDefaultSetter(value: string);
            setGetterAndSetterWithJsName(value: string): void;
            getGetterAndSetterWithJsName(): string;
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
