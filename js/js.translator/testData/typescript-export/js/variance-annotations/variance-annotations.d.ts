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
            get value(): T;
            consume(value: T): void;
        }
        namespace UnsafeCovariant {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => UnsafeCovariant<T>;
            }
        }
        class Base {
            constructor(name: string);
            get name(): string;
        }
        namespace Base {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Base;
            }
        }
        class Producer<out T extends foo.Base> {
            constructor(value: T);
            get value(): T;
        }
        namespace Producer {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Base>() => Producer<T>;
            }
        }
        interface Consumer<in T extends foo.Base> {
            consume(value: T): void;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Consumer": unique symbol;
            };
        }
        class InvariantBound<T extends foo.Base> {
            constructor(value: T);
            get value(): T;
            set value(value: T);
        }
        namespace InvariantBound {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Base>() => InvariantBound<T>;
            }
        }
        class Token {
            constructor(text: string);
            get text(): string;
        }
        namespace Token {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Token;
            }
        }
        class TokenBox<out T extends foo.Token> {
            constructor(value: T);
            get value(): T;
            get Entry(): {
                new(): TokenBox.Entry<T>;
            };
        }
        namespace TokenBox {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Token>() => TokenBox<T>;
            }
            class Entry<out T$TokenBox extends foo.Token> {
                private constructor();
                getValue(): T$TokenBox;
            }
            namespace Entry {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <T$TokenBox extends foo.Token>() => Entry<T$TokenBox>;
                }
            }
        }
    }
}
