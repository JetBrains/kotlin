declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class ClassWithoutPrimary {
            private constructor();
            get value(): string;
            static fromInt(value: number): foo.ClassWithoutPrimary;
            static fromString(value: string): foo.ClassWithoutPrimary;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace ClassWithoutPrimary.$metadata$ {
            const constructor: abstract new () => ClassWithoutPrimary;
        }
        class SomeBaseClass extends foo.ClassWithoutPrimary.$metadata$.constructor {
            private constructor();
            get answer(): number;
            static secondary(): foo.SomeBaseClass;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SomeBaseClass.$metadata$ {
            const constructor: abstract new () => SomeBaseClass;
        }
        class SomeExtendingClass extends /* foo.IntermediateClass1 */ foo.SomeBaseClass.$metadata$.constructor {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SomeExtendingClass.$metadata$ {
            const constructor: abstract new () => SomeExtendingClass;
        }
        class FinalClassInChain extends /* foo.IntermediateClass2 */ foo.SomeExtendingClass.$metadata$.constructor {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace FinalClassInChain.$metadata$ {
            const constructor: abstract new () => FinalClassInChain;
        }
    }
}