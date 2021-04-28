import { ping, Something } from "./JS_TESTS/index.js"

export default function() {
    return {
        "pingCall": function() {
            return ping(new Something())
        },
    };
};