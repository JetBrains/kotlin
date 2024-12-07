type Nullable<T> = T | null | undefined
export declare function produceBoolean(): Nullable<boolean>;
export declare function produceNumber(): Nullable<number>;
export declare function produceBigInt(): Nullable<bigint>;
export declare function produceString(): Nullable<string>;
export declare function produceAny(): unknown;
export declare function consumeBoolean(x: Nullable<boolean>): Nullable<string>;
export declare function consumeNumber(x: Nullable<number>): Nullable<string>;
export declare function consumeBigInt(x: Nullable<bigint>): Nullable<string>;
export declare function consumeString(x: Nullable<string>): Nullable<string>;
export declare function consumeAny(x: unknown): Nullable<string>;