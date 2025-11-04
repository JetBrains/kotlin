type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);

/* ErrorDeclaration: Top level function declarations are not implemented yet */
