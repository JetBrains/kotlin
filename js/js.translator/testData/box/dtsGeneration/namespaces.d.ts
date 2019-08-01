type Nullable<T> = T | null | undefined

declare namespace foo.bar.baz {
    function box(): string

    class C1 {
        constructor()
    }

    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): void
}

declare namespace a.b {
    class C2 {
        constructor()
    }

    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): void
}

declare interface C3 {
}

declare function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): void
