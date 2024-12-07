import { A, B } from "./jsExportInClass_v5.mjs";

export default function() {
    return {
        "res": (new A().ping()) + (new B().pong())
    };
};
