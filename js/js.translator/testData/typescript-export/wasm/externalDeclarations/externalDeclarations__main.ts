import { getResult } from "./index.mjs";

if (JSON.stringify(getResult()) != "{}") throw new Error("Unexpected result")