import * as api from "./interfaceWithCompanion_v5.mjs";

export default function() {
    return {
        "res": api.A.getInstance().ok()
    };
};