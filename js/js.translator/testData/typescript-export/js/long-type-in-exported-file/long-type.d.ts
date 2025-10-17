declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const _long: bigint;
        const _long_array: BigInt64Array;
        const _array_long: Array<bigint>;
        const _n_long: Nullable<bigint>;
        const funInterfaceInheritor1: foo.funInterface;
        const funInterfaceInheritor2: foo.funInterface;
        let myVar: bigint;
        function funWithLongParameters(a: bigint, b: bigint): bigint;
        function funWithLongDefaultParameters(a?: bigint, b?: bigint): bigint;
        function funWithTypeParameter<T extends bigint>(a: T, b: T): bigint;
        function funWithTypeParameterWithTwoUpperBounds<T extends unknown/* kotlin.Comparable<T> */ & bigint>(a: T, b: T): bigint;
        function funWithContextParameter(long: bigint): bigint;
        function inlineFun(a: bigint, b: bigint): bigint;
        function inlineFunWithTypeParameter<T extends bigint>(a: T, b: T): bigint;
        function inlineFunDefaultParameters(a?: bigint, b?: bigint): bigint;
        function extensionFun(_this_: bigint): bigint;
        function globalFun(a: bigint): bigint;
        abstract class objectWithLong extends KtSingleton<objectWithLong.$metadata$.constructor>() {
            private constructor();
        }
        namespace objectWithLong {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    get long(): bigint;
                    private constructor();
                }
            }
        }
        class A {
            constructor(a: bigint);
            get a(): bigint;
        }
        namespace A {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A;
            }
        }
        class B {
            private constructor();
            get b(): bigint;
            static snd_constructor(): foo.B;
        }
        namespace B {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => B;
            }
        }
        class C extends foo.A.$metadata$.constructor {
            constructor(a: bigint);
            get a(): bigint;
        }
        namespace C {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C;
            }
        }
        class D {
            constructor();
            get I(): {
                new(i: bigint): D.I;
            } & typeof D.I;
        }
        namespace D {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => D;
            }
            class N {
                constructor(n: bigint);
                get n(): bigint;
            }
            namespace N {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => N;
                }
            }
            class I {
                protected constructor($outer: foo.D, i: bigint);
                get i(): bigint;
            }
            namespace I {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => I;
                }
            }
        }
        interface funInterface {
            getLong(a: bigint): bigint;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.funInterface": unique symbol;
            };
        }
    }
}
