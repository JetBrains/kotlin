type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function createLibValue(value: number): LibValue;
export declare function echoDefaultValueClass(value: DefaultValueClass): DefaultValueClass;
export declare class LibValue {
    constructor(value: number);
    equals(other: Nullable<any>): boolean;
    hashCode(): number;
    toString(): string;
    get value(): number;
}
export declare namespace LibValue {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => LibValue;
    }
}
declare class DefaultValueClass {
    constructor(value: string);
    equals(other: Nullable<any>): boolean;
    hashCode(): number;
    toString(): string;
    get value(): string;
}
export default DefaultValueClass;
declare namespace DefaultValueClass {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => DefaultValueClass;
    }
}
export declare function echoLibValue(value: LibValue): LibValue;
export declare function createMainValue(value: number): LibValue;
export declare interface MainExternalInterface {
    echo(value: number): number;
    readonly directValue: number;
    readonly nullableValue?: Nullable<LibValue>;
}

