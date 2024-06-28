type Nullable<T> = T | null | undefined
export declare function produceUByte(): number;
export declare function produceUShort(): number;
export declare function produceUInt(): number;
export declare function produceULong(): bigint;
export declare function produceFunction(): () => number;
export declare function consumeUByte(x: number): string;
export declare function consumeUShort(x: number): string;
export declare function consumeUInt(x: number): string;
export declare function consumeULong(x: bigint): string;
export declare function consumeFunction(fn: (p0: string) => number): number;
