declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    interface Box<T, Self extends Box<T, Self>> {
        unbox(): T;
        copy(newValue: T): Self;
    }
    class GenericTestInner<T extends Box<string, T>> {
        constructor(a: T);
        get a(): T;
        get Inner(): {
            new(a: T): GenericTestInner.Inner;
            fromNumber(a: number): GenericTestInner.Inner<T>;
        };
        get GenericInner(): {
            new(a: S): GenericTestInner.GenericInner;
            fromNumber<S extends Box<R, S>, R extends string>(a: number, copier: S): GenericTestInner.GenericInner<S, R, T>;
        };
        get GenericInnerWithShadowingTP(): {
            new(a: T): GenericTestInner.GenericInnerWithShadowingTP;
            fromNumber<T extends Box<string, T>>(a: number, copier: T): GenericTestInner.GenericInnerWithShadowingTP<T, T>;
        };
        get OpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.OpenInnerWithPublicConstructor;
            fromNumber(a: number): GenericTestInner.OpenInnerWithPublicConstructor<T>;
        };
        get SubclassOfOpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.SubclassOfOpenInnerWithPublicConstructor;
        };
        get GenericOpenInnerWithPublicConstructor(): {
            new(a: S): GenericTestInner.GenericOpenInnerWithPublicConstructor;
            fromNumber<S extends Box<string, S>>(a: number, copier: S): GenericTestInner.GenericOpenInnerWithPublicConstructor<S, T>;
        };
        get SubclassOfGenericOpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.SubclassOfGenericOpenInnerWithPublicConstructor;
        };
        get GenericSubclassOfGenericOpenInnerWithPublicConstructor1(): {
            new(a: S): GenericTestInner.GenericSubclassOfGenericOpenInnerWithPublicConstructor1;
        };
        get OpenInnerWithProtectedConstructor(): {
        };
        get GenericOpenInnerWithProtectedConstructor(): {
        };
    }
    namespace GenericTestInner {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <T extends Box<string, T>>() => GenericTestInner<T>;
        }
        class Inner {
            private constructor();
            get a(): T;
            get concat(): string;
            get SecondLayerInner(): {
                new(a: T): GenericTestInner.Inner.SecondLayerInner;
            };
        }
        namespace Inner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Inner;
            }
            class SecondLayerInner {
                private constructor();
                get a(): T;
                get concat(): string;
            }
            namespace SecondLayerInner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => SecondLayerInner;
                }
            }
        }
        class GenericInner<S extends Box<R, S>, R extends string> {
            private constructor();
            get a(): S;
            get concat(): string;
            get SecondLayerGenericInner(): {
                new(a: U, v: V): GenericTestInner.GenericInner.SecondLayerGenericInner;
            };
        }
        namespace GenericInner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<R, S>, R extends string>() => GenericInner<S, R>;
            }
            class SecondLayerGenericInner<U extends Box<string, U>, V> {
                private constructor();
                get a(): U;
                get v(): V;
                get concat(): string;
            }
            namespace SecondLayerGenericInner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <U extends Box<string, U>, V>() => SecondLayerGenericInner<U, V>;
                }
            }
        }
        class GenericInnerWithShadowingTP<T extends Box<string, T>> {
            private constructor();
            get a(): T;
            get concat(): string;
        }
        namespace GenericInnerWithShadowingTP {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends Box<string, T>>() => GenericInnerWithShadowingTP<T>;
            }
        }
        class OpenInnerWithPublicConstructor {
            protected constructor($outer: GenericTestInner<T>, a: T);
            get a(): T;
            get concat(): string;
        }
        namespace OpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OpenInnerWithPublicConstructor;
            }
        }
        class SubclassOfOpenInnerWithPublicConstructor extends GenericTestInner.OpenInnerWithPublicConstructor.$metadata$.constructor<T> {
            private constructor();
        }
        namespace SubclassOfOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SubclassOfOpenInnerWithPublicConstructor;
            }
        }
        class GenericOpenInnerWithPublicConstructor<S extends Box<string, S>> {
            protected constructor($outer: GenericTestInner<T>, a: S);
            get a(): S;
            get concat(): string;
        }
        namespace GenericOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>>() => GenericOpenInnerWithPublicConstructor<S>;
            }
        }
        class SubclassOfGenericOpenInnerWithPublicConstructor extends GenericTestInner.GenericOpenInnerWithPublicConstructor.$metadata$.constructor<T, T> {
            private constructor();
        }
        namespace SubclassOfGenericOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SubclassOfGenericOpenInnerWithPublicConstructor;
            }
        }
        class GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S extends Box<string, S>> extends GenericTestInner.GenericOpenInnerWithPublicConstructor.$metadata$.constructor<S, T> {
            private constructor();
        }
        namespace GenericSubclassOfGenericOpenInnerWithPublicConstructor1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>>() => GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S>;
            }
        }
        class OpenInnerWithProtectedConstructor {
            protected constructor($outer: GenericTestInner<T>, a: T);
            get a(): T;
            get concat(): string;
        }
        namespace OpenInnerWithProtectedConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OpenInnerWithProtectedConstructor;
            }
        }
        class GenericOpenInnerWithProtectedConstructor<S extends Box<string, S>> {
            protected constructor($outer: GenericTestInner<T>, a: S);
            get a(): S;
            get concat(): string;
        }
        namespace GenericOpenInnerWithProtectedConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>>() => GenericOpenInnerWithProtectedConstructor<S>;
            }
        }
    }
}
