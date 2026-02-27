declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class Covariant<out T> {
            constructor(value: T);
            get value(): T;
        }
        namespace Covariant {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Covariant<T>;
            }
        }
        class Contravariant<in T> {
            constructor();
            consume(value: T): void;
        }
        namespace Contravariant {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Contravariant<T>;
            }
        }
        class Invariant<T> {
            constructor(value: T);
            get value(): T;
            set value(value: T);
        }
        namespace Invariant {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Invariant<T>;
            }
        }
        class UnsafeCovariant<out T> {
            constructor(value: T);
            consume(value: T): void;
            get value(): T;
        }
        namespace UnsafeCovariant {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => UnsafeCovariant<T>;
            }
        }
    }
}
