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
            parentPropertyToImplement: string;
            anotherParentMethod(): kotlin.collections.KtList<string>;
            parentAsyncMethod(): Promise<string>;
            setGetterAndSetterWithJsName(_set___: string): void;
            getGetterAndSetterWithJsName(): string;
            readonly [foo.ExportedParent.Symbol]: true;
        }
        namespace ExportedParent {
            const Symbol: unique symbol;
        }
        interface IFoo<T extends unknown/* kotlin.Comparable<T> */> extends foo.ExportedParent {
            readonly fooProperty: string;
            foo(): string;
            asyncFoo(): Promise<string>;
            withDefaults(value?: string): string;
            withBridge(x: T): T;
            readonly [foo.IFoo.Symbol]: true;
        }
        namespace IFoo {
            const Symbol: unique symbol;
        }
        function callingExportedParentMethod(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallFoo(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function callingWithDefaultsWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithBridge(foo: foo.IFoo<string>): string;
        function checkIsFooInterface(foo: any): boolean;
        function checkIsExportedParentInterface(foo: any): boolean;
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
