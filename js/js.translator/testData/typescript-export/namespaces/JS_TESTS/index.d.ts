type Nullable<T> = T | null | undefined
export class C1 {
    constructor(value: string);
    readonly value: string;
    component1(): string;
    copy(value: string): C1;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export function f1(x1: C1, x2: C2, x3: C3): string;
export class C2 {
    constructor(value: string);
    readonly value: string;
    component1(): string;
    copy(value: string): C2;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export function f2(x1: C1, x2: C2, x3: C3): string;
export class C3 {
    constructor(value: string);
    readonly value: string;
    component1(): string;
    copy(value: string): C3;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export function f3(x1: C1, x2: C2, x3: C3): string;
