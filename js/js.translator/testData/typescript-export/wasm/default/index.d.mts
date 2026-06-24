type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function produceUShort(): number;
export declare function produceUInt(): number;
declare function produceUByte(): number;
export default produceUByte;
