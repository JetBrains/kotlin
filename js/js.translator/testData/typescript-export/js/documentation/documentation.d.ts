declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    function simpleDescription(): string;
    function add(a: number, b: number): number;
    function divide(dividend: number, divisor: number): number;
    function parseIntValue(value: string): number;
    function absolute(value: number): number;
    function legacyAdd(a: number, b: number): number;
    /** @deprecated Use newFunction instead */
    function oldFunction(): void;
    function processText(input: string): string;
    class User {
        constructor(name: string, age: number);
        get name(): string;
        get age(): number;
        set age(value: number);
    }
    namespace User {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User;
        }
    }
    interface Shape {
        area(): number;
        readonly readonlyProperty: string;
        mutableProperty: string;
        readonly __doNotUseOrImplementIt: {
            readonly Shape: unique symbol;
        };
    }
    abstract class MathUtils extends KtSingleton<MathUtils.$metadata$.constructor>() {
        private constructor();
    }
    namespace MathUtils {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                get PI(): number;
                circumference(radius: number): number;
                private constructor();
            }
        }
    }
    interface WithOverridableProperty {
        readonly jsOverridableProp: string;
        overridableSetter(_set___: string): void;
        overridableGetter(): string;
        readonly __doNotUseOrImplementIt: {
            readonly WithOverridableProperty: unique symbol;
        };
    }
    class JsNamePropertyExamples implements WithOverridableProperty {
        constructor();
        get jsOverridableProp(): string;
        get renamedProp(): string;
        setCustomAccessor(value: string): void;
        getCustomAccessor(): string;
        setDefaultAccessor(_set___: string): void;
        getDefaultAccessor(): string;
        overridableSetter(value: string): void;
        overridableGetter(): string;
        readonly __doNotUseOrImplementIt: WithOverridableProperty["__doNotUseOrImplementIt"];
    }
    namespace JsNamePropertyExamples {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => JsNamePropertyExamples;
        }
    }
    function findFirst(predicate: (p0: number) => boolean): Nullable<number>;
    class User2 {
        constructor(name: string);
        get name(): string;
    }
    namespace User2 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User2;
        }
    }
    class User3 {
        constructor(name: string);
        get name(): string;
    }
    namespace User3 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User3;
        }
    }
    function box(): string;
}
