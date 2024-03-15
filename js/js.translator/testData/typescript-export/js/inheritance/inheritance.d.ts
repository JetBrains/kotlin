declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
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
        class OC extends foo.AC implements foo.I<string, boolean, number> {
            constructor(y: boolean, acAbstractProp: string);
            get y(): boolean;
            get acAbstractProp(): string;
            z(z: number): void;
        }
        class FC extends foo.OC {
            constructor();
        }
        const O1: {
        } & foo.OC;
        const O2: {
            foo(): number;
        } & foo.OC;
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
        class B2 extends foo.A2 {
            constructor();
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            get baz(): string;
            bay(): string;
        }
        class C2 extends foo.B2 {
            constructor();
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            get baz(): string;
            set baz(value: string);
            bay(): string;
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
            get name(): "EC1" | "EC2" | "EC3";
            get ordinal(): 0 | 1 | 2;
            get foo(): string;
            get bar(): string;
            set bar(value: string);
            bay(): string;
            static values(): Array<foo.EC>;
            static valueOf(value: string): foo.EC;
            abstract get baz(): string;
            readonly __doNotUseOrImplementIt: foo.I3["__doNotUseOrImplementIt"];
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
        class Third<T> extends /* foo.Second */ foo.First {
            constructor();
        }
        class Sixth extends /* foo.Fifth<number> */ foo.Third<number> implements foo.IA, foo.IG<number>/*, foo.IC */ {
            constructor();
            process(value: number): void;
            get foo(): number;
            readonly __doNotUseOrImplementIt: foo.IA["__doNotUseOrImplementIt"] & foo.IG<number>["__doNotUseOrImplementIt"];
        }
        class First {
            constructor();
        }
        function acceptForthLike<T extends (foo.Third<string> & foo.IA)/* foo.Forth<string> */>(forth: T): void;
        function acceptMoreGenericForthLike<T extends foo.IA/* foo.IB */ & foo.IA/* foo.IC */ & foo.First/* foo.Second */>(forth: T): void;
    }
}
