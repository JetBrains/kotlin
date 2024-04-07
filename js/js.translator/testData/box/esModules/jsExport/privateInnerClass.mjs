import { TableDriver } from "./privateInnerClass_v5.mjs"

export default function box() {
    var tableDriver = new TableDriver();

    return {
        value: tableDriver.foo(),
        inner: tableDriver.Table
    };
};