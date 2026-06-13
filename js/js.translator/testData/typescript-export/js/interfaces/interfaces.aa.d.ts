declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        function processInterface(test: foo.TestInterface): string;
        function processOptionalInterface(a: foo.OptionalFieldsInterface): string;
        function callComposedDefaultSuspend(value: foo.AbstractAndDefaultSuspend): Promise<string>;
        function callOuterDefaultSuspend(value: foo.ChainedDefaultSuspend): Promise<string>;
        function callDiamondDefaultSuspend(value: foo.BaseDiamondDefaultSuspend): Promise<string>;
        function callGenericDefaultSuspend(value: foo.GenericDefaultSuspend<string>, input: string): Promise<string>;
        function callChainDefaultSuspend(value: foo.ChainDefaultSuspend, input: string): Promise<string>;
        function callLeftDefaultSuspend(value: foo.LeftDefaultSuspend): Promise<string>;
        function callRightDefaultSuspend(value: foo.RightDefaultSuspend): Promise<string>;
        function callNullableDefaultSuspend(value: foo.NullableDefaultSuspend): Promise<Nullable<string>>;
        function callParameterizedDefaultSuspend(value: foo.ParameterizedDefaultSuspend, input: string): Promise<string>;
        function callUnitDefaultSuspend(value: foo.UnitDefaultSuspend): Promise<void>;
        function callParentSuspend(holder: foo.HolderOfInheritedSuspend, value: string): Promise<string>;
        function callOverrideSuspend(value: foo.OverridableSuspend): Promise<string>;
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
            readonly __doNotUseOrImplementIt: foo.AnotherExportedInterface["__doNotUseOrImplementIt"] & foo.TestInterface["__doNotUseOrImplementIt"];
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
        interface InterfaceWithJsStaticVar {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithJsStaticVar": unique symbol;
            };
        }
        namespace InterfaceWithJsStaticVar {
            let mutable: string;
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
        namespace InterfaceWithDefaultArguments {
            namespace DefaultImpls {
                function foo($this: foo.InterfaceWithDefaultArguments, x?: number): number;
                function bar($this: foo.InterfaceWithDefaultArguments, x?: number): number;
            }
        }
        class ImplementorOfInterfaceWithDefaultArguments implements foo.InterfaceWithDefaultArguments {
            constructor();
            bar(x?: number): number;
            foo(x?: number): number;
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
        }
        interface NRBase {
            readonly b: string;
        }
        interface MidClassic extends foo.NRBase {
            mid(): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.MidClassic": unique symbol;
            };
        }
        interface NRLeaf extends foo.MidClassic {
            leaf(): void;
            readonly __doNotUseOrImplementIt: foo.MidClassic["__doNotUseOrImplementIt"];
        }
        interface WithDefaultSuspend {
            regularWithDefault(): string;
            suspendWithDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.WithDefaultSuspend": unique symbol;
            };
        }
        namespace WithDefaultSuspend {
            namespace DefaultImpls {
                function regularWithDefault($this: foo.WithDefaultSuspend): string;
                function suspendWithDefault($this: foo.WithDefaultSuspend): Promise<string>;
            }
        }
        class WithDefaultSuspendImpl implements foo.WithDefaultSuspend {
            constructor();
            regularWithDefault(): string;
            suspendWithDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.WithDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace WithDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithDefaultSuspendImpl;
            }
        }
        interface AbstractAndDefaultSuspend {
            abstractSuspend(): Promise<string>;
            defaultSuspend(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.AbstractAndDefaultSuspend": unique symbol;
            };
        }
        namespace AbstractAndDefaultSuspend {
            namespace DefaultImpls {
                function defaultSuspend($this: foo.AbstractAndDefaultSuspend): Promise<string>;
            }
        }
        class AbstractAndDefaultSuspendImpl implements foo.AbstractAndDefaultSuspend {
            constructor();
            abstractSuspend(): Promise<string>;
            defaultSuspend(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.AbstractAndDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace AbstractAndDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractAndDefaultSuspendImpl;
            }
        }
        interface ChainedDefaultSuspend {
            innerSuspendDefault(): Promise<string>;
            outerSuspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ChainedDefaultSuspend": unique symbol;
            };
        }
        namespace ChainedDefaultSuspend {
            namespace DefaultImpls {
                function innerSuspendDefault($this: foo.ChainedDefaultSuspend): Promise<string>;
                function outerSuspendDefault($this: foo.ChainedDefaultSuspend): Promise<string>;
            }
        }
        class ChainedDefaultSuspendImpl implements foo.ChainedDefaultSuspend {
            constructor();
            innerSuspendDefault(): Promise<string>;
            outerSuspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.ChainedDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace ChainedDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ChainedDefaultSuspendImpl;
            }
        }
        interface BaseDiamondDefaultSuspend {
            suspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.BaseDiamondDefaultSuspend": unique symbol;
            };
        }
        namespace BaseDiamondDefaultSuspend {
            namespace DefaultImpls {
                function suspendDefault($this: foo.BaseDiamondDefaultSuspend): Promise<string>;
            }
        }
        interface LeftDiamondDefaultSuspend extends foo.BaseDiamondDefaultSuspend {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.LeftDiamondDefaultSuspend": unique symbol;
            } & foo.BaseDiamondDefaultSuspend["__doNotUseOrImplementIt"];
        }
        interface RightDiamondDefaultSuspend extends foo.BaseDiamondDefaultSuspend {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.RightDiamondDefaultSuspend": unique symbol;
            } & foo.BaseDiamondDefaultSuspend["__doNotUseOrImplementIt"];
        }
        class DiamondDefaultSuspendImpl implements foo.LeftDiamondDefaultSuspend, foo.RightDiamondDefaultSuspend {
            constructor();
            suspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.RightDiamondDefaultSuspend["__doNotUseOrImplementIt"] & foo.BaseDiamondDefaultSuspend["__doNotUseOrImplementIt"] & foo.LeftDiamondDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace DiamondDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => DiamondDefaultSuspendImpl;
            }
        }
        interface GenericDefaultSuspend<T> {
            echoSuspendDefault(input: T): Promise<T>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.GenericDefaultSuspend": unique symbol;
            };
        }
        namespace GenericDefaultSuspend {
            namespace DefaultImpls {
                function echoSuspendDefault<T>($this: foo.GenericDefaultSuspend<T>, input: T): Promise<T>;
            }
        }
        class StringGenericDefaultSuspendImpl implements foo.GenericDefaultSuspend<string> {
            constructor();
            echoSuspendDefault(input: string): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.GenericDefaultSuspend<any>["__doNotUseOrImplementIt"];
        }
        namespace StringGenericDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => StringGenericDefaultSuspendImpl;
            }
        }
        interface ChainDefaultSuspend {
            suspendDefault(input?: string): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ChainDefaultSuspend": unique symbol;
            };
        }
        namespace ChainDefaultSuspend {
            namespace DefaultImpls {
                function suspendDefault($this: foo.ChainDefaultSuspend, input?: string): Promise<string>;
            }
        }
        class MidChainDefaultSuspendImpl implements foo.ChainDefaultSuspend {
            constructor();
            suspendDefault(input?: string): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.ChainDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace MidChainDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => MidChainDefaultSuspendImpl;
            }
        }
        class LeafChainDefaultSuspendImpl extends foo.MidChainDefaultSuspendImpl.$metadata$.constructor {
            constructor();
            suspendDefault(input?: string): Promise<string>;
        }
        namespace LeafChainDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => LeafChainDefaultSuspendImpl;
            }
        }
        interface LeftDefaultSuspend {
            leftSuspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.LeftDefaultSuspend": unique symbol;
            };
        }
        namespace LeftDefaultSuspend {
            namespace DefaultImpls {
                function leftSuspendDefault($this: foo.LeftDefaultSuspend): Promise<string>;
            }
        }
        interface RightDefaultSuspend {
            rightSuspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.RightDefaultSuspend": unique symbol;
            };
        }
        namespace RightDefaultSuspend {
            namespace DefaultImpls {
                function rightSuspendDefault($this: foo.RightDefaultSuspend): Promise<string>;
            }
        }
        class MultipleInterfaceDefaultsImpl implements foo.LeftDefaultSuspend, foo.RightDefaultSuspend {
            constructor();
            leftSuspendDefault(): Promise<string>;
            rightSuspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.RightDefaultSuspend["__doNotUseOrImplementIt"] & foo.LeftDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace MultipleInterfaceDefaultsImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => MultipleInterfaceDefaultsImpl;
            }
        }
        interface NullableDefaultSuspend {
            suspendDefault(): Promise<Nullable<string>>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NullableDefaultSuspend": unique symbol;
            };
        }
        namespace NullableDefaultSuspend {
            namespace DefaultImpls {
                function suspendDefault($this: foo.NullableDefaultSuspend): Promise<Nullable<string>>;
            }
        }
        class NullableDefaultSuspendImpl implements foo.NullableDefaultSuspend {
            constructor();
            suspendDefault(): Promise<Nullable<string>>;
            readonly __doNotUseOrImplementIt: foo.NullableDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace NullableDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => NullableDefaultSuspendImpl;
            }
        }
        interface ParameterizedDefaultSuspend {
            suspendDefault(input?: string): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ParameterizedDefaultSuspend": unique symbol;
            };
        }
        namespace ParameterizedDefaultSuspend {
            namespace DefaultImpls {
                function suspendDefault($this: foo.ParameterizedDefaultSuspend, input?: string): Promise<string>;
            }
        }
        class ParameterizedDefaultSuspendImpl implements foo.ParameterizedDefaultSuspend {
            constructor();
            suspendDefault(input?: string): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.ParameterizedDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace ParameterizedDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ParameterizedDefaultSuspendImpl;
            }
        }
        interface UnitDefaultSuspend {
            runDefault(): Promise<void>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.UnitDefaultSuspend": unique symbol;
            };
        }
        namespace UnitDefaultSuspend {
            namespace DefaultImpls {
                function runDefault($this: foo.UnitDefaultSuspend): Promise<void>;
            }
        }
        class UnitDefaultSuspendImpl implements foo.UnitDefaultSuspend {
            constructor();
            runDefault(): Promise<void>;
            readonly __doNotUseOrImplementIt: foo.UnitDefaultSuspend["__doNotUseOrImplementIt"];
        }
        namespace UnitDefaultSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => UnitDefaultSuspendImpl;
            }
        }
        interface HolderOfInheritedSuspend {
            parentSuspend(value: string): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.HolderOfInheritedSuspend": unique symbol;
            };
        }
        class ExportedSuspendChild /* extends foo.HiddenSuspendParent */ {
            constructor();
            childSuspend(): Promise<string>;
        }
        namespace ExportedSuspendChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedSuspendChild;
            }
        }
        interface OverridableSuspend {
            suspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.OverridableSuspend": unique symbol;
            };
        }
        namespace OverridableSuspend {
            namespace DefaultImpls {
                function suspendDefault($this: foo.OverridableSuspend): Promise<string>;
            }
        }
        class InheritingSuspendImpl implements foo.OverridableSuspend {
            constructor();
            suspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.OverridableSuspend["__doNotUseOrImplementIt"];
        }
        namespace InheritingSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => InheritingSuspendImpl;
            }
        }
        class OverridingSuspendImpl implements foo.OverridableSuspend {
            constructor();
            suspendDefault(): Promise<string>;
            readonly __doNotUseOrImplementIt: foo.OverridableSuspend["__doNotUseOrImplementIt"];
        }
        namespace OverridingSuspendImpl {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OverridingSuspendImpl;
            }
        }
        interface ExternalInterfaceWithCompanion {
        }
        namespace ExternalInterfaceWithCompanion {
            const x: string;
        }
        interface ExternalInterfaceWithSelfTypedCompanion {
        }
        namespace ExternalInterfaceWithSelfTypedCompanion {
            const left: foo.ExternalInterfaceWithSelfTypedCompanion;
            const right: foo.ExternalInterfaceWithSelfTypedCompanion;
        }
        interface ExternalInterfaceWithIgnoredNonStaticCompanion {
        }
    }
}


