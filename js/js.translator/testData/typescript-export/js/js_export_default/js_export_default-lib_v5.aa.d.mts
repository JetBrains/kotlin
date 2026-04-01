type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);

export declare function getParent(): typeof Parent.$metadata$.type;
declare abstract class Parent {
    static readonly getInstance: () => typeof Parent.$metadata$.type;
    private constructor();
}
declare namespace Parent {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        namespace type {
            abstract class Nested1 extends KtSingleton<Nested1.$metadata$.constructor>() {
                private constructor();
            }
            namespace Nested1 {
                class Nested2 {
                    constructor();
                }
                namespace Nested2 {
                    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                    namespace $metadata$ {
                        const constructor: abstract new () => Nested2;
                    }
                    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                        private constructor();
                    }
                    namespace Companion {
                        class Nested3 {
                            constructor();
                        }
                        namespace Nested3 {
                            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                            namespace $metadata$ {
                                const constructor: abstract new () => Nested3;
                            }
                        }
                    }
                    namespace Companion {
                        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                        namespace $metadata$ {
                            abstract class constructor {
                                private constructor();
                            }
                        }
                    }
                }
            }
            namespace Nested1 {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get value(): string;
                        private constructor();
                    }
                }
            }
        }
        abstract class constructor {
            private constructor();
        }
    }
}
export default Parent;
