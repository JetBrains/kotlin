declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class RemoteValue<T extends any> {
            abstract get(): Promise<T>;
        }
        namespace RemoteValue {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends any>() => RemoteValue<T>;
            }
        }
        function createRemoteValue(): foo.RemoteValue<number>;
        function readRemoteValue(value: foo.RemoteValue<number>): Promise<number>;
    }
}
