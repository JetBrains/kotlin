declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class Simple /* implements kotlin.Annotation */ {
            constructor();
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
        }
        namespace Simple {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Simple;
            }
        }
        class WithStringParam /* implements kotlin.Annotation */ {
            constructor(message: string);
            get message(): string;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
        }
        namespace WithStringParam {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithStringParam;
            }
        }
        class WithMultipleParams /* implements kotlin.Annotation */ {
            constructor(name: string, count: number);
            get name(): string;
            get count(): number;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
        }
        namespace WithMultipleParams {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithMultipleParams;
            }
        }
        class WithDefaultValue /* implements kotlin.Annotation */ {
            constructor(level?: number);
            get level(): number;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
        }
        namespace WithDefaultValue {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithDefaultValue;
            }
        }
        class WithBooleanParam /* implements kotlin.Annotation */ {
            constructor(enabled: boolean);
            get enabled(): boolean;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
        }
        namespace WithBooleanParam {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithBooleanParam;
            }
        }
        function withIntroducedAt(x: number, y?: number, o1?: string, k?: string, o2?: string): string;
        function nonAscendingVersion(y?: number, o?: string, k?: string): void;
        function invalidParameterPosition(x: number | undefined, y: number | undefined, z: number): void;
        class ConstructorVersioning {
            constructor(x: number, y?: number, ok1?: string, ok2?: string);
            get x(): number;
            get y(): number;
            get ok1(): string;
            get ok2(): string;
            copy(x?: number, y?: number, ok1?: string, ok2?: string): foo.ConstructorVersioning;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ConstructorVersioning {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ConstructorVersioning;
            }
        }
        class ConstructorNonAscendingVersion {
            constructor(x: number, ok?: string, y?: number);
            get x(): number;
            get ok(): string;
            get y(): number;
            copy(x?: number, ok?: string, y?: number): foo.ConstructorNonAscendingVersion;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ConstructorNonAscendingVersion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ConstructorNonAscendingVersion;
            }
        }
        class ConstructorWithInvalidParameterPosition {
            constructor(x: number, y: number | undefined, z: number);
            get x(): number;
            get y(): number;
            get z(): number;
            copy(x?: number, y?: number, z?: number): foo.ConstructorWithInvalidParameterPosition;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ConstructorWithInvalidParameterPosition {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ConstructorWithInvalidParameterPosition;
            }
        }
        function box(): string;
    }
}
