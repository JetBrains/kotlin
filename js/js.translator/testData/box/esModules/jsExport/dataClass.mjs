import { Point } from "./dataClass_v5.mjs";

export default function() {
    var p = new Point(3, 7);

    return {
        "copy00": p.copy().toString(),
        "copy01": p.copy(undefined, 11).toString(),
        "copy10": p.copy(15).toString(),
        "copy11": p.copy(13, 11).toString(),
    };
};
