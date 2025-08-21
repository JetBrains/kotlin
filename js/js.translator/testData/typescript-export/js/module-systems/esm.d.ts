type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtList<E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare abstract class KtList<E> extends KtSingleton<KtList.$metadata$.constructor>() {
    private constructor();
}
export declare namespace KtList {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class constructor {
            fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
            private constructor();
        }
    }
}
export declare const value: { get(): number; };
export declare const variable: { get(): number; set(value: number): void; };
export declare class C {
    constructor(x: number);
    get x(): number;
    doubleX(): number;
}
export declare namespace C {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => C;
    }
}
export declare abstract class O {
    static readonly getInstance: () => typeof O.$metadata$.type;
    private constructor();
}
export declare namespace O {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        abstract class constructor {
            get value(): number;
            private constructor();
        }
    }
}
export declare abstract class Parent {
    static readonly getInstance: () => typeof Parent.$metadata$.type;
    private constructor();
}
export declare namespace Parent {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        namespace type {
            class Nested {
                constructor();
                get value(): number;
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        abstract class constructor {
            get value(): number;
            private constructor();
        }
    }
}
export declare function box(): string;
export declare function asyncList(): Promise<KtList<number>>;
export declare function arrayOfLists(): Array<KtList<number>>;
