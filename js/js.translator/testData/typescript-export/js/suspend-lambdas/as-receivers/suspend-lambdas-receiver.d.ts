declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const receiverSuspendLambda: (p0: string, p1: number) => Promise<string>;
        const nullableReceiverSuspendLambda: (p0: Nullable<string>) => Promise<string>;
        const classReceiverSuspendLambda: (p0: foo.Receiver) => Promise<number>;
        function callReceiverSuspendLambda(callback: (p0: string, p1: number) => Promise<string>, receiver: string, x: number): Promise<string>;
        function callNullableReceiverLambda(cb: (p0: Nullable<string>) => Promise<string>, receiver: Nullable<string>): Promise<string>;
        class Receiver {
            constructor(v: number);
            get v(): number;
        }
        namespace Receiver {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Receiver;
            }
        }
        function callClassReceiverLambda(cb: (p0: foo.Receiver) => Promise<number>, receiver: foo.Receiver): Promise<number>;
    }
}
