declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class TestDataClass {
            constructor(name: string);
            get name(): string;
            component1(): string;
            copy(name?: string): foo.TestDataClass;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace TestDataClass {
            class Nested {
                constructor();
                get prop(): string;
            }
        }
        class KT39423 {
            constructor(a: string, b?: Nullable<number>);
            get a(): string;
            get b(): Nullable<number>;
            component1(): string;
            component2(): Nullable<number>;
            copy(a?: string, b?: Nullable<number>): foo.KT39423;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
    }
}