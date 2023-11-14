declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        interface OptionalFieldsInterface {
            readonly required: number;
            readonly notRequired?: Nullable<number>;
        }
    }
    namespace foo {
        interface TestInterface {
            readonly value: string;
            getOwnerName(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.TestInterface": unique symbol;
            };
        }
        interface AnotherExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.AnotherExportedInterface": unique symbol;
            };
        }
        class TestInterfaceImpl implements foo.TestInterface {
            constructor(value: string);
            get value(): string;
            getOwnerName(): string;
            readonly __doNotUseOrImplementIt: foo.TestInterface["__doNotUseOrImplementIt"];
        }
        class ChildTestInterfaceImpl extends foo.TestInterfaceImpl implements foo.AnotherExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.TestInterfaceImpl["__doNotUseOrImplementIt"] & foo.AnotherExportedInterface["__doNotUseOrImplementIt"];
        }
        function processInterface(test: foo.TestInterface): string;
        function processOptionalInterface(a: foo.OptionalFieldsInterface): string;
        interface InterfaceWithCompanion {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithCompanion": unique symbol;
            };
        }
    }
}