type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtList<E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare namespace KtList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
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
export declare interface AnInterfaceWithCompanion {
    readonly __doNotUseOrImplementIt: {
        readonly "foo.AnInterfaceWithCompanion": unique symbol;
    };
}
export declare namespace AnInterfaceWithCompanion {
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                get someValue(): string;
                get constValue(): string;
                private constructor();
            }
        }
    }
}
export declare interface InterfaceWithCompanionWithStaticFun {
    readonly __doNotUseOrImplementIt: {
        readonly "foo.InterfaceWithCompanionWithStaticFun": unique symbol;
    };
}
export declare namespace InterfaceWithCompanionWithStaticFun {
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
export declare interface I {
    foo(): string;
    readonly __doNotUseOrImplementIt: {
        readonly "foo.I": unique symbol;
    };
}
export declare interface InterfaceWithCompanionWithInheritor {
    readonly __doNotUseOrImplementIt: {
        readonly "foo.InterfaceWithCompanionWithInheritor": unique symbol;
    };
}
export declare namespace InterfaceWithCompanionWithInheritor {
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor implements I {
                foo(): string;
                readonly __doNotUseOrImplementIt: I["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
    }
}
export declare interface InterfaceWithCompanionWithInheritorAndStaticFun {
    readonly __doNotUseOrImplementIt: {
        readonly "foo.InterfaceWithCompanionWithInheritorAndStaticFun": unique symbol;
    };
}
export declare namespace InterfaceWithCompanionWithInheritorAndStaticFun {
    function foo(): string;
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor implements I {
                readonly __doNotUseOrImplementIt: I["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
    }
}
export declare function box(): string;
export declare function asyncList(): Promise<KtList<number>>;
export declare function arrayOfLists(): Array<KtList<number>>;
declare function justSomeDefaultExport(): string;
export default justSomeDefaultExport;
