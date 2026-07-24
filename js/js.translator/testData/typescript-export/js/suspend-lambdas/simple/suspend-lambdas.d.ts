declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const exportedSuspendLambda: () => Promise<string>;
        const nullableSuspendLambda: Nullable<() => Promise<string>>;
        function produceSuspendLambda(): (p0: number) => Promise<number>;
        function produceCapturingSuspendLambda(base: number): (p0: number) => Promise<number>;
        function runLambda(callback: (p0: number) => Promise<number>): Promise<number>;
        function runVoidLambda(callback: () => Promise<void>): Promise<void>;
        function chain(a: (p0: number) => Promise<number>, b: (p0: number) => Promise<number>, x: number): Promise<number>;
        function roundTrip(callback: (p0: number) => Promise<number>): (p0: number) => Promise<number>;
        function genericRoundTrip<T>(callback: (p0: T) => Promise<T>): (p0: T) => Promise<T>;
        function callNullableSuspendLambda(callback: Nullable<(p0: number) => Promise<string>>, x: number): Promise<Nullable<string>>;
        class LambdaHolder {
            constructor(base: number);
            get multiplier(): (p0: number, p1: number) => Promise<number>;
            produceAdder(): (p0: number) => Promise<number>;
            apply(cb: (p0: number) => Promise<number>, x: number): Promise<number>;
            applyTwice(cb: (p0: number) => Promise<number>, x: number): Promise<number>;
        }
        namespace LambdaHolder {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => LambdaHolder;
            }
        }
        function callKotlinLambdaFromKotlin(): Promise<number>;
        function produceArrayOfSuspendLambdas(): Array<(p0: number) => Promise<number>>;
        function reduceArrayOfSuspendLambdas(lambdas: Array<(p0: number) => Promise<number>>, start: number): Promise<number>;
        function mapWithArrayOfSuspendLambdas(lambdas: Array<(p0: number) => Promise<number>>, x: number): Promise<Array<number>>;
        function topLevelSuspendInc(x: number): Promise<number>;
        function getSuspendDoubleRef(): (p0: number) => Promise<number>;
        function getSuspendIncRef(): (p0: number) => Promise<number>;
        class WithSuspendMethod {
            constructor(delta: number);
            addDelta(x: number): Promise<number>;
            memberRef(): (p0: number) => Promise<number>;
        }
        namespace WithSuspendMethod {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithSuspendMethod;
            }
        }
        interface InterfaceWithSuspendLambdaProp {
            readonly handler: (p0: number) => Promise<string>;
        }
        abstract class AbstractClassWithSuspendLambdaProp {
            constructor();
            abstract get handler(): (p0: number) => Promise<string>;
        }
        namespace AbstractClassWithSuspendLambdaProp {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractClassWithSuspendLambdaProp;
            }
        }
        function callHandlerFromInterface(holder: foo.InterfaceWithSuspendLambdaProp, x: number): Promise<string>;
        function callHandlerFromAbstractClass(holder: foo.AbstractClassWithSuspendLambdaProp, x: number): Promise<string>;
        function callbackThatThrows(callback: () => Promise<string>): Promise<string>;
        function throwingSuspendLambda(): Promise<string>;
        function applyAll(start: number, callbacks: Array<(p0: number) => Promise<number>>): Promise<number>;
        function withDefaultCallback(x: number, cb?: (p0: number) => Promise<number>): Promise<number>;
        function produceNestedSuspendLambda(): () => Promise<() => Promise<string>>;
        function callNestedSuspendLambda(maker: () => Promise<() => Promise<string>>): Promise<string>;
    }
}


