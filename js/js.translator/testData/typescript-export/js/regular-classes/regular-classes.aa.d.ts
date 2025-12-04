declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class A {
            constructor();
        }
        namespace A {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A;
            }
        }
        class A1 {
            constructor(x: number);
            get x(): number;
        }
        namespace A1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A1;
            }
        }
        class A2 {
            constructor(x: string, y: boolean);
            get x(): string;
            get y(): boolean;
            set y(value: boolean);
        }
        namespace A2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A2;
            }
        }
        class A3 {
            constructor();
            get x(): number;
        }
        namespace A3 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A3;
            }
        }
        class A4<T> {
            constructor(value: T);
            test(): T;
            get value(): T;
        }
        namespace A4 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => A4<T>;
            }
        }
        class A5 {
            constructor();
        }
        namespace A5 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A5;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get x(): number;
                        private constructor();
                    }
                }
            }
        }
        class A6 {
            constructor();
            then(): number;
            catch(): number;
        }
        namespace A6 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A6;
            }
        }
        class GenericClassWithConstraint<T extends foo.A6> {
            constructor(test: T);
            get test(): T;
        }
        namespace GenericClassWithConstraint {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.A6>() => GenericClassWithConstraint<T>;
            }
        }
    }
}
