declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        interface ValueProvider {
            value(x?: number): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ValueProvider": unique symbol;
            };
        }
        abstract class Base implements foo.ValueProvider {
            constructor();
            abstract value(x?: number): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.ValueProvider["__doNotUseOrImplementIt"];
        }
        namespace Base {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Base;
            }
        }
        class Implementation extends foo.Base.$metadata$.constructor {
            constructor();
            value(x?: number): Promise<string>;
        }
        namespace Implementation {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Implementation;
            }
        }
    }
}
