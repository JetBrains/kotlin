import { publicFun, Class } from "./JS_TESTS/index.js";
function box() {
    const tens = [
        publicFun(),
        new Class().publicVal,
        new Class().publicFun()
    ];
    if (!tens.every(value => value === 10))
        return "Fail 1";
    if (!(new Class() instanceof Class))
        return "Fail 2";
    // TODO: Fix nested classes
    // if (!(new Class.publicClass() instanceof Class.publicClass))
    //     return "Fail 3";
    return "OK";
}
console.assert(box() == "OK");
