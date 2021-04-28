type Nullable<T> = T | null | undefined
export function sum(x: number, y: number): number;
export function varargInt(x: Int32Array): number;
export function varargNullableInt(x: Array<Nullable<number>>): number;
export function varargWithOtherParameters(x: string, y: Array<string>, z: string): number;
export function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): number;
export function sumNullable(x: Nullable<number>, y: Nullable<number>): number;
export function defaultParameters(x: number, y: string): string;
export function generic1<T>(x: T): T;
export function generic2<T>(x: Nullable<T>): boolean;
export function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
export function inlineFun(x: number, callback: (p0: number) => void): void;
export class A {
    constructor();
}
export class A1 {
    constructor(x: number);
    readonly x: number;
}
export class A2 {
    constructor(x: string, y: boolean);
    readonly x: string;
    y: boolean;
}
export class A3 {
    constructor();
    readonly x: number;
}
export class A4 {
    constructor();
    readonly _valCustom: number;
    readonly _valCustomWithField: number;
    _varCustom: number;
    _varCustomWithField: number;
}
export class KT_37829 {
    constructor();
}