declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class A {
            constructor(a?: number, b?: string, b1?: string, c?: number);
            get a(): number;
            get b(): string;
            get c(): number;
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number, b?: string, b1?: string);
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number);
        }
        namespace A {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A;
            }
        }
        class B {
            constructor(a?: number, a1?: string, b?: string, c?: number);
            get a(): number;
            get a1(): string;
            get c(): number;
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number, b?: string, c?: number);
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number);
        }
        namespace B {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => B;
            }
        }
        class C {
            constructor(a?: number, b?: string, c?: number);
            get a(): number;
            get b(): string;
            get c(): number;
            copy(a?: number, b?: string, c?: number): foo.C;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number, b?: string);
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            constructor(a?: number);
            copy(a?: number, b?: string): foo.C;
            copy(a?: number): foo.C;
        }
        namespace C {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C;
            }
        }
        class D {
            constructor(a?: number, a1?: number, b?: string);
            get a(): number;
            get a1(): number;
            get b(): string;
            copy(a?: number, a1?: number, b?: string): foo.D;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            /** @deprecated Deprecated */
            constructor(a?: number, b?: string);
            /** @deprecated Deprecated */
            constructor(a?: number);
            copy(a?: number, b?: string): foo.D;
            copy(a?: number): foo.D;
        }
        namespace D {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => D;
            }
        }
        class X {
            constructor();
            foo(a: number, B?: string, b1?: string, c?: number): void;
            mid(a: number, a1?: number, b?: string, c?: number): void;
            foo(a: number, B?: string, b1?: string): void;
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            foo(a: number): void;
            mid(a: number, b?: string, c?: number): void;
            /** @deprecated This version is kept for binary compatibility purposes, please use the main overload. */
            mid(a: number): void;
        }
        namespace X {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => X;
            }
        }
        function foo2(a: number, B: string | undefined, b1: string | undefined, c: number | undefined, f: () => void): void;
        function mid2(a: number, a1: number | undefined, b: string | undefined, c: number | undefined, f: () => void): void;
    }
}
