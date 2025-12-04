declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function shortNameBasedDestructuring(): string;
        function fullNameBasedDestructuring(): string;
        function shortPositionBasedDestructuring(): string;
        function fullPositionBasedDestructuring(): string;
        class TestDataClass {
            constructor(name: string);
            copy(name?: string): foo.TestDataClass;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get name(): string;
        }
        namespace TestDataClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestDataClass;
            }
            class Nested {
                constructor();
                get prop(): string;
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        class KT39423 {
            constructor(a: string, b?: Nullable<number>);
            copy(a?: string, b?: Nullable<number>): foo.KT39423;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get a(): string;
            get b(): Nullable<number>;
        }
        namespace KT39423 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => KT39423;
            }
        }
        abstract class WithComponent1 {
            constructor();
            abstract component1(): string;
        }
        namespace WithComponent1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithComponent1;
            }
        }
        class Test2 extends foo.WithComponent1.$metadata$.constructor {
            constructor(value1: string, value2: string);
            component1(): string;
            copy(value1?: string, value2?: string): foo.Test2;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value1(): string;
            get value2(): string;
        }
        namespace Test2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Test2;
            }
        }
        class Test3 /* extends foo.NonExportedWithComponent1 */ {
            constructor(value1: string, value2: string);
            copy(value1?: string, value2?: string): foo.Test3;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value1(): string;
            get value2(): string;
        }
        namespace Test3 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Test3;
            }
        }
    }
}
