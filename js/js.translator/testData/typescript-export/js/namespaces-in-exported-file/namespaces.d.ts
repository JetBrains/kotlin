declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo.bar.baz {
        class C1 {
            constructor(value: string);
            get value(): string;
            copy(value?: string): foo.bar.baz.C1;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace C1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C1;
            }
        }
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
    }
    namespace a.b {
        class C2 {
            constructor(value: string);
            get value(): string;
            copy(value?: string): a.b.C2;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace C2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C2;
            }
        }
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
    }
    class C3 {
        constructor(value: string);
        get value(): string;
        copy(value?: string): C3;
        toString(): string;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
    }
    namespace C3 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => C3;
        }
    }
    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
}
