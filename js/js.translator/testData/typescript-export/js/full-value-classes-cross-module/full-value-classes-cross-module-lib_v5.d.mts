type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class LibPoint {
    constructor(x: number, y: number);
    get x(): number;
    get y(): number;
    sum(): number;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace LibPoint {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => LibPoint;
    }
}
export declare function createLibPoint(x: number, y: number): LibPoint;
export declare function echoLibPoint(point: LibPoint): LibPoint;
export declare function createMainPoint(x: number, y: number): LibPoint;
export declare function box(): string;
