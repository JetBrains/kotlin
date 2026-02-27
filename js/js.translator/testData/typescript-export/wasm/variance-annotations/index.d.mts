type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function acceptCovariant(a: not.exported.Covariant<string>): void;
export declare function acceptContravariant(a: not.exported.Contravariant<NonNullable<unknown>>): void;
export declare function acceptInvariant(a: not.exported.Invariant<number>): void;
declare namespace not.exported {
    class Covariant<out T extends NonNullable<unknown>> {
        constructor();
        get value(): T;
    }
    namespace Covariant {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <T extends NonNullable<unknown>>() => Covariant<T>;
        }
    }
}
declare namespace not.exported {
    class Contravariant<in T extends NonNullable<unknown>> {
        constructor();
        consume(value: T): void;
    }
    namespace Contravariant {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <T extends NonNullable<unknown>>() => Contravariant<T>;
        }
    }
}
declare namespace not.exported {
    class Invariant<T extends NonNullable<unknown>> {
        constructor();
        get value(): T;
        set value(value: T);
    }
    namespace Invariant {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <T extends NonNullable<unknown>>() => Invariant<T>;
        }
    }
}
