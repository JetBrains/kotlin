declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo.bar.baz {
        class C1 {
            constructor(value: string);
            get value(): string;
            copy(value?: string): foo.bar.baz.C1;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
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
    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
}
