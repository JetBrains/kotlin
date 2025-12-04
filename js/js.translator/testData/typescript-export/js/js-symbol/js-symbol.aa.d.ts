declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function ktBox(): string;
        interface ExtItf {
            [Symbol.iterator](): any;
        }
        class SymbolHost {
            constructor();
            [Symbol.toPrimitive](hint: string): string;
        }
        namespace SymbolHost {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SymbolHost;
            }
        }
        interface SymItf {
            [Symbol.toPrimitive](hint: string): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SymItf": unique symbol;
            };
        }
        class SymChild implements foo.SymItf {
            constructor();
            [Symbol.toPrimitive](hint: string): string;
            readonly __doNotUseOrImplementIt: foo.SymItf["__doNotUseOrImplementIt"];
        }
        namespace SymChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SymChild;
            }
        }
        abstract class AbsHost {
            constructor();
            abstract [Symbol.toPrimitive](hint: string): string;
        }
        namespace AbsHost {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbsHost;
            }
        }
        class AbsChild extends foo.AbsHost.$metadata$.constructor {
            constructor();
            [Symbol.toPrimitive](hint: string): string;
        }
        namespace AbsChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbsChild;
            }
        }
        class IterHost implements foo.ExtItf {
            constructor();
            [Symbol.iterator](): any;
        }
        namespace IterHost {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => IterHost;
            }
        }
    }
}
