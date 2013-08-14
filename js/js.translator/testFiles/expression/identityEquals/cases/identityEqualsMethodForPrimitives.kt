package foo

fun box(): String {
    if (!null.identityEquals(null)) return "null !== null"
    if (!("ab" identityEquals "ab")) return "ab !== ab"
    if (("ab" identityEquals "a")) return "ab === a"

    if ("0" identityEquals 0) return "'0' === 0"
    if (!(0 identityEquals 0)) return "0 !== 0"
    if (0 identityEquals 1) return "0 === 1"


    return "OK";
}