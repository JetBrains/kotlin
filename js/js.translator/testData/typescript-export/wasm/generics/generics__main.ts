import {complexConstraint, second, simple, simpleWithConstraint, nothingInTypeArgument } from "./index.mjs";

if (simple("OK") != "OK") throw new Error("Unexpected result from `simple` function")
if (second(1, "OK") != "OK") throw new Error("Unexpected result from `second` function")
if (simpleWithConstraint(42) != 42) throw new Error("Unexpected result from `simpleConstraint` function")
if (JSON.stringify(complexConstraint({ foo: 1n, bar: "bar" })) != "{\"baz\":true}") throw new Error("Unexpected result from `complexConstraint` function")

var thrown = false;
try {
    nothingInTypeArgument().boo();
    thrown = false;
} catch(e) {
    thrown = true;
}
if (!thrown) throw new Error("Unexpected result from `nothingInTypeArgument` function");