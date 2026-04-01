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
            new(a: T): GenericTestInner.Inner<T>;
            fromNumber(a: number): GenericTestInner.Inner<T>;
        };
        get GenericInner(): {
            new<S extends Box<R, S>, R extends string>(a: S): GenericTestInner.GenericInner<S, R, T>;
            fromNumber<S extends Box<R, S>, R extends string>(a: number, copier: S): GenericTestInner.GenericInner<S, R, T>;
        };
        get GenericInnerWithShadowingTP(): {
            new<T_0 extends Box<string, T_0>>(a: T_0): GenericTestInner.GenericInnerWithShadowingTP<T_0, T>;
            fromNumber<T_0 extends Box<string, T_0>>(a: number, copier: T_0): GenericTestInner.GenericInnerWithShadowingTP<T_0, T>;
        };
        get OpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.OpenInnerWithPublicConstructor<T>;
            fromNumber(a: number): GenericTestInner.OpenInnerWithPublicConstructor<T>;
        };
        get SubclassOfOpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.SubclassOfOpenInnerWithPublicConstructor<T>;
        };
        get GenericOpenInnerWithPublicConstructor(): {
            new<S extends Box<string, S>>(a: S): GenericTestInner.GenericOpenInnerWithPublicConstructor<S, T>;
            fromNumber<S extends Box<string, S>>(a: number, copier: S): GenericTestInner.GenericOpenInnerWithPublicConstructor<S, T>;
        };
        get SubclassOfGenericOpenInnerWithPublicConstructor(): {
            new(a: T): GenericTestInner.SubclassOfGenericOpenInnerWithPublicConstructor<T>;
        };
        get GenericSubclassOfGenericOpenInnerWithPublicConstructor1(): {
            new<S extends Box<string, S>>(a: S): GenericTestInner.GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S, T>;
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
        class Inner<T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            private constructor();
            get a(): T$GenericTestInner;
            get concat(): string;
            get SecondLayerInner(): {
                new(a: T$GenericTestInner): GenericTestInner.Inner.SecondLayerInner<T$GenericTestInner>;
            };
        }
        namespace Inner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => Inner<T$GenericTestInner>;
            }
            class SecondLayerInner<T$GenericTestInner extends Box<string, T$GenericTestInner>> {
                private constructor();
                get a(): T$GenericTestInner;
                get concat(): string;
            }
            namespace SecondLayerInner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => SecondLayerInner<T$GenericTestInner>;
                }
            }
        }
        class GenericInner<S extends Box<R, S>, R extends string, T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            private constructor();
            get a(): S;
            get concat(): string;
            get SecondLayerGenericInner(): {
                new<U extends Box<string, U>, V>(a: U, v: V): GenericTestInner.GenericInner.SecondLayerGenericInner<U, V, S, R, T$GenericTestInner>;
            };
        }
        namespace GenericInner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<R, S>, R extends string, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => GenericInner<S, R, T$GenericTestInner>;
            }
            class SecondLayerGenericInner<U extends Box<string, U>, V, S$GenericInner$GenericTestInner extends Box<R$GenericInner$GenericTestInner, S$GenericInner$GenericTestInner>, R$GenericInner$GenericTestInner extends string, T$GenericTestInner extends Box<string, T$GenericTestInner>> {
                private constructor();
                get a(): U;
                get v(): V;
                get concat(): string;
            }
            namespace SecondLayerGenericInner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <U extends Box<string, U>, V, S$GenericInner$GenericTestInner extends Box<R$GenericInner$GenericTestInner, S$GenericInner$GenericTestInner>, R$GenericInner$GenericTestInner extends string, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => SecondLayerGenericInner<U, V, S$GenericInner$GenericTestInner, R$GenericInner$GenericTestInner, T$GenericTestInner>;
                }
            }
        }
        class GenericInnerWithShadowingTP<T extends Box<string, T>, T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            private constructor();
            get a(): T;
            get concat(): string;
        }
        namespace GenericInnerWithShadowingTP {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends Box<string, T>, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => GenericInnerWithShadowingTP<T, T$GenericTestInner>;
            }
        }
        class OpenInnerWithPublicConstructor<T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            protected constructor($outer: GenericTestInner<T$GenericTestInner>, a: T$GenericTestInner);
            get a(): T$GenericTestInner;
            get concat(): string;
        }
        namespace OpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => OpenInnerWithPublicConstructor<T$GenericTestInner>;
            }
        }
        class SubclassOfOpenInnerWithPublicConstructor<T$GenericTestInner extends Box<string, T$GenericTestInner>> extends GenericTestInner.OpenInnerWithPublicConstructor.$metadata$.constructor<T$GenericTestInner> {
            private constructor();
        }
        namespace SubclassOfOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => SubclassOfOpenInnerWithPublicConstructor<T$GenericTestInner>;
            }
        }
        class GenericOpenInnerWithPublicConstructor<S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            protected constructor($outer: GenericTestInner<T$GenericTestInner>, a: S);
            get a(): S;
            get concat(): string;
        }
        namespace GenericOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => GenericOpenInnerWithPublicConstructor<S, T$GenericTestInner>;
            }
        }
        class SubclassOfGenericOpenInnerWithPublicConstructor<T$GenericTestInner extends Box<string, T$GenericTestInner>> extends GenericTestInner.GenericOpenInnerWithPublicConstructor.$metadata$.constructor<T$GenericTestInner, T$GenericTestInner> {
            private constructor();
        }
        namespace SubclassOfGenericOpenInnerWithPublicConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => SubclassOfGenericOpenInnerWithPublicConstructor<T$GenericTestInner>;
            }
        }
        class GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>> extends GenericTestInner.GenericOpenInnerWithPublicConstructor.$metadata$.constructor<S, T$GenericTestInner> {
            private constructor();
        }
        namespace GenericSubclassOfGenericOpenInnerWithPublicConstructor1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S, T$GenericTestInner>;
            }
        }
        class OpenInnerWithProtectedConstructor<T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            protected constructor($outer: GenericTestInner<T$GenericTestInner>, a: T$GenericTestInner);
            get a(): T$GenericTestInner;
            get concat(): string;
        }
        namespace OpenInnerWithProtectedConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T$GenericTestInner extends Box<string, T$GenericTestInner>>() => OpenInnerWithProtectedConstructor<T$GenericTestInner>;
            }
        }
        class GenericOpenInnerWithProtectedConstructor<S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>> {
            protected constructor($outer: GenericTestInner<T$GenericTestInner>, a: S);
            get a(): S;
            get concat(): string;
        }
        namespace GenericOpenInnerWithProtectedConstructor {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <S extends Box<string, S>, T$GenericTestInner extends Box<string, T$GenericTestInner>>() => GenericOpenInnerWithProtectedConstructor<S, T$GenericTestInner>;
            }
        }
    }
}
