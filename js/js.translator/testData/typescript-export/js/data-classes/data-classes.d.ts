declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class TestDataClass {
            constructor(name: string);
            get name(): string;
            copy(name?: string): foo.TestDataClass;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace TestDataClass.$metadata$ {
            const constructor: abstract new () => TestDataClass;
        }
        namespace TestDataClass {
            class Nested {
                constructor();
                get prop(): string;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        class KT39423 {
            constructor(a: string, b?: Nullable<number>);
            get a(): string;
            get b(): Nullable<number>;
            copy(a?: string, b?: Nullable<number>): foo.KT39423;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace KT39423.$metadata$ {
            const constructor: abstract new () => KT39423;
        }
        abstract class WithComponent1 {
            constructor();
            abstract component1(): string;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace WithComponent1.$metadata$ {
            const constructor: abstract new () => WithComponent1;
        }
        class Test2 extends foo.WithComponent1.$metadata$.constructor {
            constructor(value1: string, value2: string);
            get value1(): string;
            get value2(): string;
            component1(): string;
            copy(value1?: string, value2?: string): foo.Test2;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Test2.$metadata$ {
            const constructor: abstract new () => Test2;
        }
    }
}