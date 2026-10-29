declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        function anonymous(): foo.Base;
        function named(): foo.Base;
        function callBase(base: foo.Base): Promise<string>;
        function createRemoteValue(): foo.RemoteValue<number>;
        function readRemoteValue(value: foo.RemoteValue<number>): Promise<number>;
        function callCombinedBase(value: foo.BaseWithInterface, x: number): Promise<string>;
        function callCombinedInterface(value: foo.ValueProvider, x: number): Promise<string>;
        abstract class Base {
            constructor();
            abstract value(x: number): Promise<string>;
        }
        namespace Base {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Base;
            }
        }
        abstract class RemoteValue<T extends any> {
            abstract get(): Promise<T>;
        }
        namespace RemoteValue {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends any>() => RemoteValue<T>;
            }
        }
        abstract class BaseWithInterface {
            constructor();
            abstract value(x: number): Promise<string>;
        }
        namespace BaseWithInterface {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => BaseWithInterface;
            }
        }
        interface ValueProvider {
            value(x: number): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ValueProvider": unique symbol;
            };
        }
        class CombinedImplementation extends foo.BaseWithInterface.$metadata$.constructor implements foo.ValueProvider {
            constructor();
            value(x: number): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.ValueProvider["__doNotUseOrImplementIt"];
        }
        namespace CombinedImplementation {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => CombinedImplementation;
            }
        }
    }
}
