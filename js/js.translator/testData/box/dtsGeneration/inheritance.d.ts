type Nullable<T> = T | null | undefined

declare namespace foo {
    function box(): string

    interface I<T, S, U> {
        x: T;

        readonly y: S;

        z(u: U): void
    }

    interface I2 {
        x: string;

        readonly y: boolean;

        z(z: number): void
    }

    abstract class AC implements foo.I2 {
        constructor()

        x: string;

        readonly y: boolean;

        abstract z(z: number): void

        readonly acProp: string;

        readonly acAbstractProp: string;
    }

    class OC extends foo.AC implements foo.I<string, boolean, number> {
        constructor(y: boolean, acAbstractProp: string)

        readonly y: boolean;

        readonly acAbstractProp: string;

        z(z: number): void
    }

    class FC extends foo.OC {
        constructor()
    }
}
