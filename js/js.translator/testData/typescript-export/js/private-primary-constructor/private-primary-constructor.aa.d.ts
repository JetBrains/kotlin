declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class ClassWithoutPrimary {
            private constructor();
            static fromInt(value: number): foo.ClassWithoutPrimary;
            static fromString(value: string): foo.ClassWithoutPrimary;
            get value(): string;
        }
        namespace ClassWithoutPrimary {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithoutPrimary;
            }
        }
        class SomeBaseClass extends foo.ClassWithoutPrimary.$metadata$.constructor {
            private constructor();
            static secondary(): foo.SomeBaseClass;
            get answer(): number;
        }
        namespace SomeBaseClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SomeBaseClass;
            }
        }
        class SomeExtendingClass /* extends foo.IntermediateClass1 */ {
            private constructor();
        }
        namespace SomeExtendingClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SomeExtendingClass;
            }
        }
        class FinalClassInChain /* extends foo.IntermediateClass2 */ {
            constructor();
        }
        namespace FinalClassInChain {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => FinalClassInChain;
            }
        }
    }
}
