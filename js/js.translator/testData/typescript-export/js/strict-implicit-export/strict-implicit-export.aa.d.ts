declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }

    namespace foo {
        const console: Console;
        const error: CompileError;
        const forth: any/* foo.Forth */;
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        function childProducer(value: number): any/* foo.NotExportedChildClass */;
        function childConsumer(value: any/* foo.NotExportedChildClass */): number;
        function genericChildProducer<T extends unknown/* foo.NonExportedGenericType<number> */>(value: T): any/* foo.NotExportedChildGenericClass<T> */;
        function genericChildConsumer<T extends unknown/* foo.NonExportedGenericType<number> */>(value: any/* foo.NotExportedChildGenericClass<T> */): T;
        function baz(a: number): Promise<number>;
        function bazVoid(a: number): Promise<void>;
        function bar(): Error;
        function pep<T extends unknown/* foo.NonExportedInterface */ & unknown/* foo.NonExportedGenericInterface<number> */>(x: T): void;
        function acceptForthLike<T extends unknown/* foo.Forth */>(forth: T): void;
        function acceptMoreGenericForthLike<T extends unknown/* foo.IB */ & unknown/* foo.IC */ & foo.Third>(forth: T): void;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}
