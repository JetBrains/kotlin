import { A, B } from "./main/index.js";

export default function() {
    return {
        "res": (new A().ping()) + (new B().pong())
    };
};
