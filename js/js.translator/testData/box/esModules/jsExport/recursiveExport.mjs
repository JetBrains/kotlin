import { ping, Something } from "./main/index.js"

export default function() {
    return {
        "pingCall": function() {
            return ping(new Something())
        },
    };
};