import Class = JS_TESTS.Class;

function box(): string {
    const tens: number[] = [
        JS_TESTS.publicVal,
        JS_TESTS.publicFun(),
        new JS_TESTS.Class().publicVal,
        new JS_TESTS.Class().publicFun(),
        //@ts-ignore
        new JS_TESTS.Class().protectedAbstractFun(),
        //@ts-ignore
        new JS_TESTS.Class().protectedAbstractVal,
        //@ts-ignore
        JS_TESTS.FinalClass.fromString("").protectedAbstractFun(),
        //@ts-ignore
        JS_TESTS.FinalClass.fromString("").protectedAbstractVal,
    ];

    if (!tens.every(value => value === 10))
        return "Error: Public values and functions should all return 10";
    if (!(new Class() instanceof Class))
        return "Error: Class constructor should create an instance of Class";
    if (!(new Class.publicClass() instanceof Class.publicClass))
        return "Error: Nested public class constructor should create an instance of the nested class";
    return "OK";
}
