declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class WithIgnoredCompanion {
            constructor();
            static bar(): string;
            static get foo(): string;
            static get baz(): string;
            static get mutable(): string;
            static set mutable(value: string);
            static staticSuspend(): Promise<string>;
        }
        namespace WithIgnoredCompanion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithIgnoredCompanion;
            }
        }
        class WithoutIgnoredCompanion {
            constructor();
            static bar(): string;
            static get foo(): string;
            static get baz(): string;
            static get mutable(): string;
            static set mutable(value: string);
            static staticSuspend(): Promise<string>;
        }
        namespace WithoutIgnoredCompanion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithoutIgnoredCompanion;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        hidden(): string;
                        get delegated(): string;
                        companionSuspend(): Promise<string>;
                        private constructor();
                    }
                }
            }
        }
    }
}
