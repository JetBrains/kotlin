import {
    consumeBoolean,
    consumeByte,
    consumeChar,
    consumeFunction,
    consumeInt,
    consumeLong,
    consumeShort,
    consumeString,
    produceBoolean,
    produceByte,
    produceChar,
    produceFunction,
    produceInt,
    produceLong,
    produceShort,
    produceString,
} from "./index.mjs";

// PRODUCING
if (!produceBoolean()) throw new Error("Unexpected value was returned from the `produceBoolean` function")
if (produceByte() != 127) throw new Error("Unexpected value was returned from the `produceByte` function")
if (produceShort() != 32767) throw new Error("Unexpected value was returned from the `produceShort` function")
if (produceInt() != 2147483647) throw new Error("Unexpected value was returned from the `produceInt` function")
if (produceLong() != 9223372036854775807n) throw new Error("Unexpected value was returned from the `produceLong` function")
if (String.fromCharCode(produceChar() ?? -1) != "a") throw new Error("Unexpected value was returned from the `produceChar` function")
if (produceString() != "OK") throw new Error("Unexpected value was returned from the `produceString` function")
if (produceFunction()?.() != 42) throw new Error("Unexpected value was returned from the `produceFunction` function")

// CONSUMPTION
if (consumeBoolean(false) != "false") throw new Error("Unexpected value was returned from the `consumeBoolean` function")
if (consumeByte(-128) != "-128") throw new Error("Unexpected value was returned from the `consumeByte` function")
if (consumeShort(-32768) != "-32768") throw new Error("Unexpected value was returned from the `consumeShort` function")
if (consumeInt(-2147483648) != "-2147483648") throw new Error("Unexpected value was returned from the `consumeInt` function")
if (consumeLong(-9223372036854775808n) != "-9223372036854775808") throw new Error("Unexpected value was returned from the `consumeLong` function")
if (consumeChar("b".charCodeAt(0)) != "b") throw new Error("Unexpected value was returned from the `consumeChar` function")
if (consumeString("🙂") != "🙂") throw new Error("Unexpected value was returned from the `consumeString` function")
if (consumeFunction(parseInt) != 42) throw new Error("Unexpected value was returned from the `consumeFunction` function")
