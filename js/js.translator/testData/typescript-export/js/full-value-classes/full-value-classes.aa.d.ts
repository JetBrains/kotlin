declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        function createPoint(x: number, y: number): foo.Point;
        function acceptPoint(point: foo.Point): number;
        function echoNullablePoint(point: Nullable<foo.Point>): Nullable<foo.Point>;
        function echoPoints(points: Array<foo.Point>): Array<foo.Point>;
        function echoLabeled<T>(value: foo.Labeled<T>): foo.Labeled<T>;
        function pointAsync(x: number, y: number): Promise<foo.Point>;
        class Point {
            constructor(x: number, y: number);
            static createFromValue(value: number): foo.Point;
            sum(): number;
            scaledSum(scale: number): number;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get x(): number;
            get y(): number;
        }
        namespace Point {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Point;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        origin(): foo.Point;
                        private constructor();
                    }
                }
            }
        }
        interface PointConsumer {
            consume(point: foo.Point): number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.PointConsumer": unique symbol;
            };
        }
        class DefaultPointConsumer implements foo.PointConsumer {
            constructor();
            consume(point: foo.Point): number;
            readonly __doNotUseOrImplementIt: foo.PointConsumer["__doNotUseOrImplementIt"];
        }
        namespace DefaultPointConsumer {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => DefaultPointConsumer;
            }
        }
        class Labeled<T> {
            constructor(value: T, label: string);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): T;
            get label(): string;
        }
        namespace Labeled {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Labeled<T>;
            }
        }
    }
}
