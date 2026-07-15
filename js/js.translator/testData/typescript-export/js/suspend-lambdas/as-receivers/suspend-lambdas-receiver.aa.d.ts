declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const receiverSuspendLambda: (_this_: string, p1: number) => Promise<string>;
        const nullableReceiverSuspendLambda: (_this_: Nullable<string>) => Promise<string>;
        const classReceiverSuspendLambda: (_this_: foo.Receiver) => Promise<number>;
        function callReceiverSuspendLambda(callback: (_this_: string, p1: number) => Promise<string>, receiver: string, x: number): Promise<string>;
        function callNullableReceiverLambda(cb: (_this_: Nullable<string>) => Promise<string>, receiver: Nullable<string>): Promise<string>;
        function callClassReceiverLambda(cb: (_this_: foo.Receiver) => Promise<number>, receiver: foo.Receiver): Promise<number>;
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
    }
}
