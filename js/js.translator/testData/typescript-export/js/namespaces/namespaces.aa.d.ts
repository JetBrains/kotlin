declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
    class C3 {
        constructor(value: string);
        copy(value?: string): C3;
        equals(other: Nullable<any>): boolean;
        hashCode(): number;
        toString(): string;
        get value(): string;
    }
    namespace C3 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => C3;
        }
    }
    namespace a.b {
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
        class C2 {
            constructor(value: string);
            copy(value?: string): a.b.C2;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): string;
        }
        namespace C2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C2;
            }
        }
    }
    namespace foo.bar.baz {
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
        class C1 {
            constructor(value: string);
            copy(value?: string): foo.bar.baz.C1;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): string;
        }
        namespace C1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C1;
            }
        }
    }
}
