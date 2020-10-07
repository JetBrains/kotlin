declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        function sum(x: number, y: number): number;
        function varargInt(x: Int32Array): number;
        function varargNullableInt(x: Array<Nullable<number>>): number;
        function varargWithOtherParameters(x: string, y: Array<string>, z: string): number;
        function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): number;
        function sumNullable(x: Nullable<number>, y: Nullable<number>): number;
        function defaultParameters(x: number, y: string): string;
        function generic1<T>(x: T): T;
        function generic2<T>(x: Nullable<T>): boolean;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
        function inlineFun(x: number, callback: (p0: number) => void): void;
        const _const_val: number;
        const _val: number;
        let _var: number;
        const _valCustom: number;
        const _valCustomWithField: number;
        let _varCustom: number;
        let _varCustomWithField: number;
        class A {
            constructor();
        }
        class A1 {
            constructor(x: number);
            readonly x: number;
        }
        class A2 {
            constructor(x: string, y: boolean);
            readonly x: string;
            y: boolean;
        }
        class A3 {
            constructor();
            readonly x: number;
        }
        class A4 {
            constructor();
            readonly _valCustom: number;
            readonly _valCustomWithField: number;
            _varCustom: number;
            _varCustomWithField: number;
        }
        const O0: {
        };
        const O: {
            readonly x: number;
            foo(): number;
        };
        function takesO(o: typeof foo.O): number;
        class KT_37829 {
            constructor();
            readonly Companion: {
                readonly x: number;
            };
        }
    }
}
