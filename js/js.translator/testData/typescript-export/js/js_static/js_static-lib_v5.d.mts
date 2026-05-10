type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class WithIgnoredCompanion {
    constructor();
    static bar(): string;
    static get foo(): string;
    static get baz(): string;
    static get mutable(): string;
    static set mutable(value: string);
    static staticSuspend(): Promise<string>;
}
export declare namespace WithIgnoredCompanion {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => WithIgnoredCompanion;
    }
}
export declare class WithoutIgnoredCompanion {
    constructor();
    static bar(): string;
    static get foo(): string;
    static get baz(): string;
    static get mutable(): string;
    static set mutable(value: string);
    static staticSuspend(): Promise<string>;
}
export declare namespace WithoutIgnoredCompanion {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => WithoutIgnoredCompanion;
    }
    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
        private constructor();
    }
    namespace Companion {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            abstract class constructor {
                hidden(): string;
                get delegated(): string;
                companionSuspend(): Promise<string>;
                private constructor();
            }
        }
    }
}
export declare abstract class ObjectWithJsStatic {
    static readonly getInstance: () => typeof ObjectWithJsStatic.$metadata$.type;
    private constructor();
    static bar(): string;
    static get foo(): string;
    static get baz(): string;
    static get mutable(): string;
    static set mutable(value: string);
    static staticSuspend(): Promise<string>;
}
export declare namespace ObjectWithJsStatic {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        abstract class constructor {
            hidden(): string;
            get delegated(): string;
            companionSuspend(): Promise<string>;
            private constructor();
        }
    }
}
