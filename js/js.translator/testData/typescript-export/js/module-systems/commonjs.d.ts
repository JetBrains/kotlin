type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin.collections {
    interface KtList<E> /* extends kotlin.collections.Collection<E> */ {
        asJsReadonlyArrayView(): ReadonlyArray<E>;
        readonly __doNotUseOrImplementIt: {
            readonly "kotlin.collections.KtList": unique symbol;
        };
    }
    abstract class KtList<E> extends KtSingleton<KtList.$metadata$.constructor>() {
        private constructor();
    }
    namespace KtList {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
                private constructor();
            }
        }
    }
}
export declare namespace foo {
    const prop: number;
    class C {
        constructor(x: number);
        get x(): number;
        doubleX(): number;
    }
    namespace C {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => C;
        }
    }
    function box(): string;
    function asyncList(): Promise<kotlin.collections.KtList<number>>;
    function arrayOfLists(): Array<kotlin.collections.KtList<number>>;
}