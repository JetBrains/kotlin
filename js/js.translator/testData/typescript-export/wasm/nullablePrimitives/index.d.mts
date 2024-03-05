type Nullable<T> = T | null | undefined
export declare function produceBoolean(): Nullable<boolean>;
export declare function produceByte(): Nullable<number>;
export declare function produceShort(): Nullable<number>;
export declare function produceInt(): Nullable<number>;
export declare function produceLong(): Nullable<bigint>;
export declare function produceChar(): Nullable<number>;
export declare function produceString(): Nullable<string>;
export declare function produceFunction(): Nullable<() => number>;
export declare function consumeBoolean(x: Nullable<boolean>): Nullable<string>;
export declare function consumeByte(x: Nullable<number>): Nullable<string>;
export declare function consumeShort(x: Nullable<number>): Nullable<string>;
export declare function consumeInt(x: Nullable<number>): Nullable<string>;
export declare function consumeLong(x: Nullable<bigint>): Nullable<string>;
export declare function consumeChar(x: Nullable<number>): Nullable<string>;
export declare function consumeString(x: Nullable<string>): Nullable<string>;
export declare function consumeFunction(fn: Nullable<(p0: string) => number>): Nullable<number>;