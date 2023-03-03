declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        /** @deprecated message 2 */
        const bar: string;
        /** @deprecated message 1 */
        function foo(): void;
        /** @deprecated message 3 */
        class TestClass {
            constructor();
        }
        class AnotherClass {
            /** @deprecated message 4 */
            constructor(value: string);
            get value(): string;
            /** @deprecated message 5 */
            static fromNothing(): foo.AnotherClass;
            static fromInt(value: number): foo.AnotherClass;
            /** @deprecated message 6 */
            foo(): void;
            baz(): void;
            /** @deprecated message 7 */
            get bar(): string;
        }
        interface TestInterface {
            /** @deprecated message 8 */
            foo(): void;
            bar(): void;
            /** @deprecated message 9 */
            readonly baz: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.TestInterface": unique symbol;
            };
        }
        const TestObject: {
            /** @deprecated message 10 */
            foo(): void;
            bar(): void;
            /** @deprecated message 11 */
            get baz(): string;
        };
    }
}
