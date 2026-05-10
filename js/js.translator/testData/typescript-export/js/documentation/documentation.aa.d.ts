declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    /**
     * A simple exported function with just a description.
     */
    function simpleDescription(): string;
    /**
     * Adds two numbers.
     *
     * This function performs **addition** of two integers.
     * See [Kotlin documentation](https://kotlinlang.org) for more details.
     *
     * @param a - The first number.
     * Must be a valid 32-bit integer.
     * @param b - The second number.
     * Must also be a valid 32-bit integer.
     * @returns The sum of `a` and `b`
     */
    function add(a: number, b: number): number;
    /**
     * Divides two numbers.
     *
     * Performs *floating-point* division.
     *
     * ### Important
     *
     * If the divisor is zero, the behavior depends on the platform.
     *
     * @param dividend - The dividend.
     * This is the number to be divided.
     * @param divisor - The divisor.
     * This is the number by which `dividend` is divided.
     * Must **not** be zero.
     * @returns The result of dividing `dividend` by `divisor`.
     * Returns `Infinity` for division by zero on some platforms.
     * @throws ArithmeticException if divisor is zero
     */
    function divide(dividend: number, divisor: number): number;
    /**
     * Parses a string to integer.
     * @param value - The string value
     * @returns The parsed integer
     * @throws NumberFormatException if not a valid integer
     */
    function parseIntValue(value: string): number;
    /**
     * Gets absolute value.
     * @param value - Input number
     * @returns The absolute value
     */
    function absolute(value: number): number;
    /**
     * Legacy addition function.
     * @see add
     * @since 1.0.0
     */
    function legacyAdd(a: number, b: number): number;
    /**
     * Old function.
     * @deprecated Use newFunction instead
     */
    function oldFunction(): void;
    /**
     * A function with a multiline description.
     *
     * This demonstrates how multiline KDoc descriptions
     * are translated into TSDoc format.
     *
     * Here is a code example:
     *
     * ```
     * val result = processText("hello")
     * println(result) // HELLO
     * ```
     *
     * And a list of transformations applied:
     * - Trim whitespace
     * - Convert to **uppercase**
     * - Encode to *UTF-8*
     *
     * @param input - The input string.
     * May contain any valid [Unicode](https://unicode.org) characters.
     * @returns The processed result.
     * Guaranteed to be non-empty if `input` is non-empty.
     */
    function processText(input: string): string;
    /**
     * Finds the first matching element.
     *
     * Iterates through a predefined collection and returns
     * the **first** element satisfying the given `predicate`.
     *
     * ### Usage
     *
     * ```
     * val result = findFirst { it > 5 }
     * ```
     *
     * See also [MathUtils] for numerical utilities.
     *
     * @param predicate - The matching condition.
     * A lambda that receives an `Int` and returns `true`
     * if the element matches.
     * @returns The first match, or `null` if no element satisfies the predicate.
     * Note: the return type is *nullable*.
     * @throws IllegalArgumentException if the internal list is empty
     * @see MathUtils
     * @since 1.5.0
     */
    function findFirst(predicate: (p0: number) => boolean): Nullable<number>;
    function box(): string;
    /**
     * Represents a user.
     *
     * A data holder for user information used across the application.
     * Refer to the [User guide](https://example.com/users) for best practices.
     *
     * ### Example
     *
     * ```
     * val user = User("Alice", 30)
     * ```
     *
     */
    class User {
        /**
         * Creates a new user instance with the given `name` and `age`.
         * @param name - The user's name.
         * Should be a **non-empty** string.
         * @param age - The user's age.
         * Must be a *non-negative* integer.
         */
        constructor(name: string, age: number);
        /**
         * The user's name.
         * Should be a **non-empty** string.
         */
        get name(): string;
        /**
         * The user's age.
         * Must be a *non-negative* integer.
         */
        get age(): number;
        set age(value: number);
    }
    namespace User {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User;
        }
    }
    /**
     * A shape abstraction.
     *
     */
    interface Shape {
        /**
         * Computes the area.
         * @returns The area as a double
         */
        area(): number;
        /**
         * An internal property that should not appear in generated documentation.
         * @ignore
         */
        readonly readonlyProperty: string;
        /**
         * A mutable property excluded from public API docs.
         * @ignore
         */
        mutableProperty: string;
        readonly __doNotUseOrImplementIt: {
            readonly Shape: unique symbol;
        };
    }
    /**
     * Math utility singleton.
     * @since 2.0.0
     */
    abstract class MathUtils extends KtSingleton<MathUtils.$metadata$.constructor>() {
        private constructor();
    }
    namespace MathUtils {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                /**
                 * Computes circle circumference.
                 * @param radius - The circle radius
                 * @returns The circumference
                 */
                circumference(radius: number): number;
                /**
                 * The value of PI.
                 */
                get PI(): number;
                private constructor();
            }
        }
    }
    /**
     * An interface with a property intended to be overridden with a custom JS name.
     */
    interface WithOverridableProperty {
        /**
         * A property to be overridden with a custom JS name in the implementing class.
         */
        readonly jsOverridableProp: string;
        /**
         * A base property whose getter and setter have custom JS names.
         */
        overridableSetter(value: string): void;
        /**
         * A base property whose getter and setter have custom JS names.
         */
        overridableGetter(): string;
        readonly __doNotUseOrImplementIt: {
            readonly WithOverridableProperty: unique symbol;
        };
    }
    /**
     * Demonstrates @JsName applied to class properties in four distinct ways.
     */
    class JsNamePropertyExamples implements WithOverridableProperty {
        /**
         * Demonstrates @JsName applied to class properties in four distinct ways.
         */
        constructor();
        /**
         * A property to be overridden with a custom JS name in the implementing class.
         */
        get jsOverridableProp(): string;
        /**
         * Case 2: @JsName on the property itself (non-override, backing field).
         * Renames the generated JS getter.
         */
        get renamedProp(): string;
        /**
         * Case 3: @JsName on custom getter and setter.
         * Each accessor is exposed under its own JS name.
         */
        setCustomAccessor(value: string): void;
        /**
         * Case 3: @JsName on custom getter and setter.
         * Each accessor is exposed under its own JS name.
         */
        getCustomAccessor(): string;
        /**
         * Case 4: @JsName on the default getter and setter via use-site targets.
         * Both accessors are exposed as plain JS functions.
         */
        setDefaultAccessor(value: string): void;
        /**
         * Case 4: @JsName on the default getter and setter via use-site targets.
         * Both accessors are exposed as plain JS functions.
         */
        getDefaultAccessor(): string;
        /**
         * An overridden property whose getter and setter have custom JS names.
         */
        overridableSetter(value: string): void;
        /**
         * An overridden property whose getter and setter have custom JS names.
         */
        overridableGetter(): string;
        readonly __doNotUseOrImplementIt: WithOverridableProperty["__doNotUseOrImplementIt"];
    }
    namespace JsNamePropertyExamples {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => JsNamePropertyExamples;
        }
    }
    class User2 {
        /**
         * @param name - The user's name.
         */
        constructor(name: string);
        /**
         * The user's name.
         */
        get name(): string;
    }
    namespace User2 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User2;
        }
    }
    class User3 {
        /**
         * @param name - Defined description by param tag.
         */
        constructor(name: string);
        /**
         * Defined description by param tag.
         */
        get name(): string;
    }
    namespace User3 {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => User3;
        }
    }
}
