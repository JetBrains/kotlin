type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin {
    class Pair<out A, out B> /* implements kotlin.io.Serializable */ {
        constructor(first: A, second: B);
        toString(): string;
        copy(first?: A, second?: B): kotlin.Pair<A, B>;
        equals(other: Nullable<any>): boolean;
        hashCode(): number;
        get first(): A;
        get second(): B;
    }
    namespace Pair {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <A, B>() => Pair<A, B>;
        }
    }
}
export declare namespace foo {
    const prop: number;
    function box(): string;
    function asyncList(): Promise<any/* kotlin.collections.List<number> */>;
    function arrayOfLists(): Array<any/* kotlin.collections.List<number> */>;
    function acceptArrayOfPairs(array: Array<kotlin.Pair<string, string>>): void;
    function justSomeDefaultExport(): string;
    class C {
        constructor(x: number);
        doubleX(): number;
        get x(): number;
    }
    namespace C {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => C;
        }
    }
    interface InterfaceWithCompanionWithStaticFun {
        readonly __doNotUseOrImplementIt: {
            readonly "foo.InterfaceWithCompanionWithStaticFun": unique symbol;
        };
    }
    namespace InterfaceWithCompanionWithStaticFun {
        function bar(): string;
        abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
            private constructor();
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

