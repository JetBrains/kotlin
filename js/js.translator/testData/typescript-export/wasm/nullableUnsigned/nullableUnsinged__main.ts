import {
    consumeFunction,
    consumeUByte,
    consumeUInt,
    consumeULong,
    consumeUShort,
    produceFunction,
    produceUByte,
    produceUInt,
    produceULong,
    produceUShort
} from "./index.mjs";

// PRODUCING
if (produceUByte() != 255) throw new Error("Unexpected value was returned from the `produceUByte` function")
if (produceUShort() != 65535) throw new Error("Unexpected value was returned from the `produceUShort` function")
if (produceUInt() != 4294967295) throw new Error("Unexpected value was returned from the `produceUInt` function")
if (produceULong() != 18446744073709551615n) throw new Error("Unexpected value was returned from the `produceULong` function")
if (produceFunction()() != 4294967295) throw new Error("Unexpected value was returned from the `produceFunction` function")

// CONSUMPTION
if (consumeUByte(-128) != "128") throw new Error("Unexpected value was returned from the `consumeUByte` function")
if (consumeUShort(-32768) != "32768") throw new Error("Unexpected value was returned from the `consumeUShort` function")
if (consumeUInt(-2147483648) != "2147483648") throw new Error("Unexpected value was returned from the `consumeUInt` function")
if (consumeULong(-9223372036854775808n) != "9223372036854775808") throw new Error("Unexpected value was returned from the `consumeULong` function")
if (consumeFunction(parseInt) != 42) throw new Error("Unexpected value was returned from the `consumeFunction` function")
