type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class LibValue {
    constructor(value: number);
    get value(): number;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace LibValue {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => LibValue;
    }
}
export declare function createLibValue(value: number): LibValue;
export declare function echoDefaultValueClass(value: DefaultValueClass): DefaultValueClass;
declare class DefaultValueClass {
    constructor(value: string);
    get value(): string;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export default DefaultValueClass;
declare namespace DefaultValueClass {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => DefaultValueClass;
    }
}
export declare interface MainExternalInterface {
    readonly directValue: number;
    readonly nullableValue?: Nullable<LibValue>;
    echo(value: number): number;
}
export declare function echoLibValue(value: LibValue): LibValue;
export declare function createMainValue(value: number): LibValue;
export declare function box(): string;
