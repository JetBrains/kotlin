import { bar } from "./packages_v5.mjs";

export default function() {
    var o_bar = bar(); // KT-60832: No way to specify `O.bar()`
    var k_bar = bar(); // KT-60832: No way to specify `K.bar()`
    return {
        "res": o_bar + k_bar
    };
};
