import {
    produceUByte,
    produceUInt,
    produceUShort
} from "./index.mjs"

// PRODUCING
if (produceUByte() != 255) throw new Error("Unexpected value was returned from the `produceUByte` function")
if (produceUShort() != 65535) throw new Error("Unexpected value was returned from the `produceUShort` function")
if (produceUInt() != 4294967295) throw new Error("Unexpected value was returned from the `produceUInt` function")
