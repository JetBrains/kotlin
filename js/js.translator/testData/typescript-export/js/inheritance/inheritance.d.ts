declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        interface I<T, S, U> {
            x?: T;
            readonly y?: S;
            z(u: U): void;
        }
        interface I2 {
            x: string;
            readonly y: boolean;
            z(z: number): void;
        }
    }
    namespace foo {
        const fifth: (foo.Third<boolean> & foo.IA & foo.IG<boolean>)/* foo.Fifth<boolean> */;
        abstract class AC implements foo.I2 {
            constructor();
            get x(): string;
            set x(value: string);
            abstract get y(): boolean;
            abstract z(z: number): void;
            get acProp(): string;
            abstract get acAbstractProp(): string;
        }
        namespace AC {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AC;
            }
        }
        class OC extends foo.AC.$metadata$.constructor implements foo.I<string, boolean, number> {
            constructor(y: boolean, acAbstractProp: string);
            get y(): boolean;
            get acAbstractProp(): string;
            z(z: number): void;
        }
        namespace OC {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OC;
            }
        }
        class FC extends foo.OC.$metadata$.constructor {
            constructor();
        }
        namespace FC {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => FC;
            }
        }
        abstract class O1 extends KtSingleton<O1.$metadata$.constructor>() {
            private constructor();
        }
        namespace O1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.OC.$metadata$.constructor {
                    private constructor();
                }
            }
        }
        abstract class O2 extends KtSingleton<O2.$metadata$.constructor>() {
            private constructor();
        }
        namespace O2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.OC.$metadata$.constructor {
                    foo(): number;
                    private constructor();
                }
            }
        }
        abstract class O3 extends KtSingleton<O3.$metadata$.constructor>() {
            private constructor();
        }
        namespace O3 {
            class SomeNestedClass {
                constructor();
            }
            namespace SomeNestedClass {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => SomeNestedClass;
                }
            }
        }
        namespace O3 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.OC.$metadata$.constructor {
                    foo(): number;
                    private constructor();
                }
            }
        }
        interface I3 {
            readonly foo: string;
            bar: string;
            readonly baz: string;
            bay(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.I3": unique symbol;
            };
        }
        function getI3(): foo.I3;
        function getA(): foo.I3;
        function getB(): foo.I3;
        function getC(): foo.I3;
        abstract class A2 implements foo.I3 {
            constructor();
            abstract get foo(): string;
            abstract get bar(): string;
            abstract set bar(value: string);
            abstract get baz(): string;
            abstract bay(): string;
            readonly __doNotUseOrImplementIt: foo.I3["__doNotUseOrImplementIt"];
        }
        namespace A2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A2;
            }
        }
        class B2 extends foo.A2.$metadata$.constructor {
            constructor();
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            get baz(): string;
            bay(): string;
        }
        namespace B2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => B2;
            }
        }
        class C2 extends foo.B2.$metadata$.constructor {
            constructor();
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            get baz(): string;
            set baz(value: string);
            bay(): string;
        }
        namespace C2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C2;
            }
        }
        abstract class EC implements foo.I3 {
            private constructor();
            static get EC1(): foo.EC & {
                get name(): "EC1";
                get ordinal(): 0;
            };
            static get EC2(): foo.EC & {
                get name(): "EC2";
                get ordinal(): 1;
            };
            static get EC3(): foo.EC & {
                get name(): "EC3";
                get ordinal(): 2;
            };
            static values(): [typeof foo.EC.EC1, typeof foo.EC.EC2, typeof foo.EC.EC3];
            static valueOf(value: string): foo.EC;
            get name(): "EC1" | "EC2" | "EC3";
            get ordinal(): 0 | 1 | 2;
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            bay(): string;
            abstract get baz(): string;
            readonly __doNotUseOrImplementIt: foo.I3["__doNotUseOrImplementIt"];
        }
        namespace EC {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => EC;
            }
        }
        interface IA {
            readonly foo: any;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IA": unique symbol;
            };
        }
        interface IG<T> {
            process(value: T): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IG": unique symbol;
            };
        }
        class Third<T> extends /* foo.Second */ foo.First.$metadata$.constructor {
            constructor();
        }
        namespace Third {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Third<T>;
            }
        }
        class Sixth extends /* foo.Fifth<number> */ foo.Third.$metadata$.constructor<number> implements foo.IA, foo.IG<number>/*, foo.IC */ {
            constructor();
            process(value: number): void;
            get foo(): number;
            readonly __doNotUseOrImplementIt: foo.IA["__doNotUseOrImplementIt"] & foo.IG<number>["__doNotUseOrImplementIt"];
        }
        namespace Sixth {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Sixth;
            }
        }
        class First {
            constructor();
        }
        namespace First {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => First;
            }
        }
        function acceptForthLike<T extends (foo.Third<string> & foo.IA)/* foo.Forth<string> */>(forth: T): void;
        function acceptMoreGenericForthLike<T extends foo.IA/* foo.IB */ & foo.IA/* foo.IC */ & foo.First/* foo.Second */>(forth: T): void;
        class MyRootException extends /* kotlin.RuntimeException */ Error {
            constructor();
        }
        namespace MyRootException {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => MyRootException;
            }
        }
        class MySpecificException extends foo.MyRootException.$metadata$.constructor {
            constructor();
        }
        namespace MySpecificException {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => MySpecificException;
            }
        }
    }
}
