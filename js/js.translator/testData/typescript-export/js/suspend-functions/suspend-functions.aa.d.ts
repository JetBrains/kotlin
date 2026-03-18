declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function sum(x: number, y: number): Promise<number>;
        function varargInt(x: Int32Array): Promise<number>;
        function varargNullableInt(x: Array<Nullable<number>>): Promise<number>;
        function varargWithOtherParameters(x: string, y: Array<string>, z: string): Promise<number>;
        function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): Promise<number>;
        function sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
        function defaultParameters(a: string, x?: number, y?: string): Promise<string>;
        function generic1<T>(x: T): Promise<T>;
        function generic2<T>(x: Nullable<T>): Promise<boolean>;
        function genericWithConstraint<T extends string>(x: T): Promise<T>;
        function genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): Promise<T>;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<Nullable<E>>;
        function inlineFun(x: number, callback: (p0: number) => number): Promise<number>;
        function simpleSuspendFun(x: number): Promise<number>;
        function inlineChain(x: number): Promise<number>;
        function suspendExtensionFun(_this_: number): Promise<number>;
        function suspendFunWithContext(ctx: number): Promise<number>;
        function acceptHolderOfSum(test: foo.HolderOfSum): Promise<void>;
        function generateOneMoreChildOfTest(): foo.Test;
        function acceptTest(test: foo.Test): Promise<void>;
        function getHolderOfParentSuspendFun1(): foo.HolderOfParentSuspendFun1<string>;
        function acceptExportedChild(child: foo.ExportedChild): Promise<void>;
        function acceptHolderOfParentSuspendFun1(holder: foo.HolderOfParentSuspendFun1<string>): Promise<void>;
        class WithSuspendExtensionFunAndContext {
            constructor();
            suspendFun(ctx: number, _this_: number): Promise<number>;
        }
        namespace WithSuspendExtensionFunAndContext {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithSuspendExtensionFunAndContext;
            }
        }
        class WithSuspendFunInsideInnerClass {
            constructor();
            get Inner(): {
                new(): foo.WithSuspendFunInsideInnerClass.Inner;
            };
        }
        namespace WithSuspendFunInsideInnerClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithSuspendFunInsideInnerClass;
            }
            class Inner {
                private constructor();
                suspendFun(): Promise<number>;
            }
            namespace Inner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Inner;
                }
            }
        }
        interface SomeExternalInterface {
        }
        interface HolderOfSum {
            sum(x: number, y: number): Promise<number>;
            sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
            defaultSum(x: number, y: number): Promise<number>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.HolderOfSum": unique symbol;
            };
        }
        class Test implements foo.HolderOfSum {
            constructor();
            sum(x: number, y: number): Promise<number>;
            varargInt(x: Int32Array): Promise<number>;
            varargNullableInt(x: Array<Nullable<number>>): Promise<number>;
            varargWithOtherParameters(x: string, y: Array<string>, z: string): Promise<number>;
            varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): Promise<number>;
            sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
            defaultParameters(a: string, x?: number, y?: string): Promise<string>;
            generic1<T>(x: T): Promise<T>;
            generic2<T>(x: Nullable<T>): Promise<boolean>;
            genericWithConstraint<T extends string>(x: T): Promise<T>;
            genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): Promise<T>;
            generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<Nullable<E>>;
            readonly __doNotUseOrImplementIt: foo.HolderOfSum["__doNotUseOrImplementIt"];
        }
        namespace Test {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Test;
            }
        }
        class TestChild extends foo.Test.$metadata$.constructor {
            constructor();
            varargInt(x: Int32Array): Promise<number>;
            sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
            generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<E>;
        }
        namespace TestChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestChild;
            }
        }
        interface HolderOfParentSuspendFun1<T> {
            parentSuspendFun1(someValue?: string): Promise<T>;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.HolderOfParentSuspendFun1": unique symbol;
            };
        }
        class ExportedChild /* extends foo.NotExportedParent */ {
            constructor();
            childSuspendFun(): Promise<string>;
        }
        namespace ExportedChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedChild;
            }
        }
    }
}
