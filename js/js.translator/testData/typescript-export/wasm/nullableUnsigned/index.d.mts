type Nullable<T> = T | null | undefined
export declare function produceUByte(): Nullable<number>;
export declare function produceUShort(): Nullable<number>;
export declare function produceUInt(): Nullable<number>;
export declare function produceULong(): Nullable<bigint>;
export declare function produceFunction(): () => Nullable<number>;
export declare function consumeUByte(x: Nullable<number>): Nullable<string>;
export declare function consumeUShort(x: Nullable<number>): Nullable<string>;
export declare function consumeUInt(x: Nullable<number>): Nullable<string>;
export declare function consumeULong(x: Nullable<bigint>): Nullable<string>;
export declare function consumeFunction(fn: (p0: string) => Nullable<number>): Nullable<number>;
