declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class ClassWithoutPrimary {
            private constructor();
            get value(): string;
            static fromInt(value: number): foo.ClassWithoutPrimary;
            static fromString(value: string): foo.ClassWithoutPrimary;
        }
        /* @ts-ignore: extends class with private primary constructor */
        class SomeBaseClass extends foo.ClassWithoutPrimary {
            private constructor();
            get answer(): number;
            static secondary(): foo.SomeBaseClass;
        }
        /* @ts-ignore: extends class with private primary constructor */
        class SomeExtendingClass extends /* foo.IntermediateClass1 */ foo.SomeBaseClass {
            private constructor();
        }
        /* @ts-ignore: extends class with private primary constructor */
        class FinalClassInChain extends /* foo.IntermediateClass2 */ foo.SomeExtendingClass {
            constructor();
        }
    }
}