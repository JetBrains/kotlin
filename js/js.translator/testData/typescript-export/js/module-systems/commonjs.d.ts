type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin.collections {
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
export declare namespace kotlin {
    class Pair<A, B> /* implements kotlin.io.Serializable */ {
        constructor(first: A, second: B);
        get first(): A;
        get second(): B;
        toString(): string;
        copy(first?: A, second?: B): kotlin.Pair<A, B>;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
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
    function acceptArrayOfPairs(array: Array<kotlin.Pair<string, string>>): void;
    function justSomeDefaultExport(): string;
}