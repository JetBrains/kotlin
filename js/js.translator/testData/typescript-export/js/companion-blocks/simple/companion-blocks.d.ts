declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class ExportedWithCompanionBlock {
            constructor();
            static append(value?: string): string;
            static get readOnly(): string;
            static get mutable(): string;
            static set mutable(value: string);
            appendToInstance(value?: string): string;
            get instanceReadOnly(): string;
            get instanceMutable(): string;
            set instanceMutable(value: string);
        }
        namespace ExportedWithCompanionBlock {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedWithCompanionBlock;
            }
        }
        class ExportedBase {
            constructor();
            static shared(): string;
            static baseOnly(): string;
        }
        namespace ExportedBase {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedBase;
            }
        }
        class ExportedChild extends foo.ExportedBase.$metadata$.constructor {
            constructor();
            static shared(): string;
            static childOnly(): string;
        }
        namespace ExportedChild {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedChild;
            }
        }
        interface ExportedInterfaceWithCompanion {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterfaceWithCompanion": unique symbol;
            };
        }
        namespace ExportedInterfaceWithCompanion {
            function interfaceFun(): string;
        }
    }
}
