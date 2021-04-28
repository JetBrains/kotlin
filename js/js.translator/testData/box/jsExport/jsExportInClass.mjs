import { A, B } from "./JS_TESTS/index.js";

export default function() {
    return {
        "res": (new A().ping()) + (new B().pong())
    };
};
