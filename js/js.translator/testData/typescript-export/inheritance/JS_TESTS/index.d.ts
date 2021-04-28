type Nullable<T> = T | null | undefined
export interface I<T, S, U> {
    x: T;
    readonly y: S;
    z(u: U): void;
}
export interface I2 {
    x: string;
    readonly y: boolean;
    z(z: number): void;
}
export class FC extends OC {
    constructor();
}
export abstract class AC implements I2 {
    constructor();
    x: string;
    abstract readonly y: boolean;
    abstract z(z: number): void;
    readonly acProp: string;
    abstract readonly acAbstractProp: string;
}
export class OC extends AC implements I<string, boolean, number> {
    constructor(y: boolean, acAbstractProp: string);
    readonly y: boolean;
    readonly acAbstractProp: string;
    z(z: number): void;
}