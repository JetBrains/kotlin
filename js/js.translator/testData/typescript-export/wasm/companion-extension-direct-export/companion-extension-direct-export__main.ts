import { extensionAppend } from "./index.mjs"

if (extensionAppend("L") !== "OL") throw new Error("FAIL: extension argument")
