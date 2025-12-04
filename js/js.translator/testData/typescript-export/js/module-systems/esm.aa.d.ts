type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);


export declare const value: {
    get(): number;
};
export declare let variable: {
    get(): number;
    set(value: number): void;
};
export declare function box(): string;
export declare function asyncList(): Promise<any/* List<number> */>;
export declare function arrayOfLists(): Array<any/* List<number> */>;
export declare function justSomeDefaultExport(): string;
export declare class C {
    constructor(x: number);
    doubleX(): number;
    get x(): number;
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
                constructor(value: number);
                static fromString(s: string): Parent.$metadata$.type.Nested;
                get value(): number;
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
            abstract class NestedEnum {
                private constructor();
                static get A(): Parent.$metadata$.type.NestedEnum & {
                    get name(): "A";
                    get ordinal(): 0;
                };
                static get B(): Parent.$metadata$.type.NestedEnum & {
                    get name(): "B";
                    get ordinal(): 1;
                };
                static values(): [typeof Parent.$metadata$.type.NestedEnum.A, typeof Parent.$metadata$.type.NestedEnum.B];
                static valueOf(value: string): Parent.$metadata$.type.NestedEnum;
                get name(): "A" | "B";
                get ordinal(): 0 | 1;
            }
            namespace NestedEnum {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => NestedEnum;
                }
            }
            abstract class NestedObject extends KtSingleton<NestedObject.$metadata$.constructor>() {
                private constructor();
            }
            namespace NestedObject {
                class Nested {
                    constructor(value: number);
                    static fromString(s: string): Parent.$metadata$.type.NestedObject.Nested;
                    get value(): number;
                }
                namespace Nested {
                    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                    namespace $metadata$ {
                        const constructor: abstract new () => Nested;
                    }
                }
            }
            namespace NestedObject {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get value(): number;
                        private constructor();
                    }
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