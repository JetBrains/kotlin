type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);

export declare function getParent(): typeof Parent.$metadata$.type;
/* ErrorDeclaration: Class declarations are not implemented yet */