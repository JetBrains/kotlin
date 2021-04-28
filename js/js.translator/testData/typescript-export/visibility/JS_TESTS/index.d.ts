type Nullable<T> = T | null | undefined
export interface publicInterface {
}
export function publicFun(): number;
export class publicClass {
    constructor();
}
export class Class {
    constructor();
    readonly publicVal: number;
    publicFun(): number;
}