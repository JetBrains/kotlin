type Nullable<T> = T | null | undefined
export declare function produceBoolean(): boolean;
export declare function produceByte(): number;
export declare function produceShort(): number;
export declare function produceInt(): number;
export declare function produceLong(): bigint;
export declare function produceChar(): number;
export declare function produceString(): string;
export declare function getState(): string;
export declare function mutateState(): void;
export declare function produceFunction(): () => number;
export declare function consumeBoolean(x: boolean): string;
export declare function consumeByte(x: number): string;
export declare function consumeShort(x: number): string;
export declare function consumeInt(x: number): string;
export declare function consumeLong(x: bigint): string;
export declare function consumeChar(x: number): string;
export declare function consumeString(x: string): string;
export declare function consumeFunction(fn: (p0: string) => number): number;
