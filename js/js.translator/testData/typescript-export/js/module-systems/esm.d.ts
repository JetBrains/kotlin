type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtList<out E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare namespace KtList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
}
export declare const value: {
    get(): number;
};
export declare const variable: {
    get(): number;
    set(_set___: number): void;
};
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
    static someStaticFunction(): string;
    static get someStaticProperty(): number;
    static set someStaticProperty(value: number);
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
    class Nested {
        constructor(value: number);
        get value(): number;
        static fromString(s: string): Parent.$metadata$.type.Nested;
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
            get value(): number;
            static fromString(s: string): Parent.$metadata$.type.NestedObject.Nested;
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
export declare namespace Parent {
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
export declare interface InterfaceWithNamedCompanion {
    readonly __doNotUseOrImplementIt: {
        readonly "foo.InterfaceWithNamedCompanion": unique symbol;
    };
}
export declare namespace InterfaceWithNamedCompanion {
    const staticValue: string;
    abstract class Name extends KtSingleton<Name.$metadata$.constructor>() {
        private constructor();
    }
    namespace Name {
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
export declare interface InterfaceWithNestedClass {
    createNested(value: number): InterfaceWithNestedClass.Nested;
    consumeNested(nested: InterfaceWithNestedClass.Nested): number;
    readonly __doNotUseOrImplementIt: {
        readonly "foo.InterfaceWithNestedClass": unique symbol;
    };
}
export declare namespace InterfaceWithNestedClass {
    class Nested {
        constructor(value: number);
        get value(): number;
    }
    namespace Nested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => Nested;
        }
        class DeepNested {
            constructor(value: string);
            get value(): string;
        }
        namespace DeepNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => DeepNested;
            }
        }
    }
    class GenericNested<T> {
        constructor(value: T);
        get value(): T;
    }
    namespace GenericNested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <T>() => GenericNested<T>;
        }
    }
    class DataNested {
        constructor(value: string);
        get value(): string;
        copy(value?: string): InterfaceWithNestedClass.DataNested;
        toString(): string;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
    }
    namespace DataNested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => DataNested;
        }
    }
    abstract class AbstractNested {
        constructor();
        abstract box(): string;
    }
    namespace AbstractNested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => AbstractNested;
        }
    }
    class ConcreteNested extends InterfaceWithNestedClass.AbstractNested.$metadata$.constructor {
        constructor();
        box(): string;
    }
    namespace ConcreteNested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ConcreteNested;
        }
    }
    class ConstructorWithDefaultsAndVarargs {
        constructor(prefix: string | undefined, parts: Array<string>);
        get prefix(): string;
        get parts(): Array<string>;
    }
    namespace ConstructorWithDefaultsAndVarargs {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ConstructorWithDefaultsAndVarargs;
        }
    }
    class NestedValue {
        constructor(value: number);
        get value(): number;
        toString(): string;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
    }
    namespace NestedValue {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => NestedValue;
        }
    }
}
export declare interface FunInterfaceWithNestedClass {
    run(value: string): string;
    readonly __doNotUseOrImplementIt: {
        readonly "foo.FunInterfaceWithNestedClass": unique symbol;
    };
}
export declare namespace FunInterfaceWithNestedClass {
    class Nested {
        constructor(value: string);
        get value(): string;
    }
    namespace Nested {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => Nested;
        }
    }
}
export declare function createInterfaceNested(value: number): InterfaceWithNestedClass.Nested;
export declare function consumeInterfaceNested(nested: InterfaceWithNestedClass.Nested): number;
export declare function createGenericInterfaceNested(value: string): InterfaceWithNestedClass.GenericNested<string>;
export declare function copyInterfaceDataNested(nested: InterfaceWithNestedClass.DataNested): InterfaceWithNestedClass.DataNested;
export declare function createDeepInterfaceNested(value: string): InterfaceWithNestedClass.Nested.DeepNested;
export declare function createConcreteInterfaceNested(): InterfaceWithNestedClass.ConcreteNested;
export declare function createValueInterfaceNested(value: number): InterfaceWithNestedClass.NestedValue;
export declare function createFunInterfaceNested(value: string): FunInterfaceWithNestedClass.Nested;
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
