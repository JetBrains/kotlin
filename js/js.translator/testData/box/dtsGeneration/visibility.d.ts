type Nullable<T> = T | null | undefined

declare function box(): string

declare const publicVal: number;

declare function publicFun(): number

declare class publicClass {
    constructor()
}

declare interface publicInterface {
}

declare class Class {
    constructor()

    readonly publicVal: number;

    publicFun(): number
}

declare namespace Class {
    class publicClass {
        constructor()
    }
}
