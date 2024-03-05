type Nullable<T> = T | null | undefined
export declare const value: { get(): number; };
export declare const variable: { get(): number; set(value: number): void; };
export declare class C {
    constructor(x: number);
    get x(): number;
    doubleX(): number;
}
export declare const O: {
    getInstance(): {
        get value(): number;
    };
};
export declare const Parent: {
    getInstance(): typeof __NonExistentParent;
};
declare abstract class __NonExistentParent extends _objects_.foo$Parent {
    private constructor();
}
declare namespace __NonExistentParent {
    class Nested {
        constructor();
        get value(): number;
    }
}
export declare function box(): string;
declare namespace _objects_ {
    const foo$Parent: {
        get value(): number;
    } & {
        new(): any;
    };
}