// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    if (null !== null) return "null !== null"
    if (!("ab" === "ab")) return "ab !== ab"
    if ("ab" === "a") return "ab === a"

    if ("0" as Any === 0) return "'0' === 0"
    if (!(0 === 0)) return "0 !== 0"
    if (0 === 1) return "0 === 1"


    return "OK";
}