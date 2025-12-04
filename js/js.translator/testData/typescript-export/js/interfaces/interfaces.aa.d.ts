declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function processInterface(test: foo.TestInterface): string;
        function processOptionalInterface(a: foo.OptionalFieldsInterface): string;
        interface TestInterface {
            getOwnerName(): string;
            readonly value: string;
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
            getOwnerName(): string;
            get value(): string;
            readonly __doNotUseOrImplementIt: foo.TestInterface["__doNotUseOrImplementIt"];
        }
        namespace TestInterfaceImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestInterfaceImpl;
            }
        }
        class ChildTestInterfaceImpl extends foo.TestInterfaceImpl.$metadata$.constructor implements foo.AnotherExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.TestInterfaceImpl["__doNotUseOrImplementIt"] & foo.AnotherExportedInterface["__doNotUseOrImplementIt"];
        }
        namespace ChildTestInterfaceImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ChildTestInterfaceImpl;
            }
        }
        interface OptionalFieldsInterface {
            readonly required: number;
            readonly notRequired?: Nullable<number>;
        }
        interface WithTheCompanion {
            readonly interfaceField: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.WithTheCompanion": unique symbol;
            };
        }
        namespace WithTheCompanion {
            function companionStaticFunction(): string;
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        companionFunction(): string;
                        private constructor();
                    }
                }
            }
        }
        interface KT83930 {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.KT83930": unique symbol;
            };
        }
        namespace KT83930 {
            const hello: string;
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
        interface InterfaceWithCompanion {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithCompanion": unique symbol;
            };
        }
        interface InterfaceWithNamedCompanion {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithNamedCompanion": unique symbol;
            };
        }
        namespace InterfaceWithNamedCompanion {
            function companionStaticFunction(): string;
            abstract class Named extends KtSingleton<Named.$metadata$.constructor>() {
                private constructor();
            }
            namespace Named {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        companionFunction(): string;
                        private constructor();
                    }
                }
            }
        }
        interface SomeSealedInterface {
            readonly x: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SomeSealedInterface": unique symbol;
            };
        }
        namespace SomeSealedInterface {
            class SomeNestedImpl implements foo.SomeSealedInterface {
                constructor(x: string);
                copy(x?: string): foo.SomeSealedInterface.SomeNestedImpl;
                equals(other: Nullable<any>): boolean;
                hashCode(): number;
                toString(): string;
                get x(): string;
                readonly __doNotUseOrImplementIt: foo.SomeSealedInterface["__doNotUseOrImplementIt"];
            }
            namespace SomeNestedImpl {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => SomeNestedImpl;
                }
            }
        }
        interface ExportedParentInterface {
        }
        interface ExportedChildInterface extends foo.ExportedParentInterface {
            bar(): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedChildInterface": unique symbol;
            };
        }
        interface InterfaceWithDefaultArguments {
            foo(x?: number): number;
            bar(x?: number): number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithDefaultArguments": unique symbol;
            };
        }
        class ImplementorOfInterfaceWithDefaultArguments implements foo.InterfaceWithDefaultArguments {
            constructor();
            bar(x?: number): number;
            readonly __doNotUseOrImplementIt: foo.InterfaceWithDefaultArguments["__doNotUseOrImplementIt"];
        }
        namespace ImplementorOfInterfaceWithDefaultArguments {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ImplementorOfInterfaceWithDefaultArguments;
            }
        }
        interface NoRuntimeSimpleInterface {
            readonly x: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NoRuntimeSimpleInterface": unique symbol;
            };
        }
        interface NRBase {
            readonly b: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NRBase": unique symbol;
            };
        }
        interface MidClassic extends foo.NRBase {
            mid(): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.MidClassic": unique symbol;
            } & foo.NRBase["__doNotUseOrImplementIt"];
        }
        interface NRLeaf extends foo.MidClassic {
            leaf(): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NRLeaf": unique symbol;
            } & foo.MidClassic["__doNotUseOrImplementIt"];
        }
    }
}
