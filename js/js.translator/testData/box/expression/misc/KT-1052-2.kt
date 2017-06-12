// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    if (!true.and(true)) {
        return "fail1"
    }
    if (false.and(true)) {
        return "fail2"
    }

    if (!true.or(false)) {
        return "fail3"
    }


    if (false.or(false)) {
        return "fail3"
    }


    if (!true.or(true)) {
        return "fail4"
    }


    if (false.and(false)) {
        return "fail5"
    }

    if (false.xor(false)) {
        return "fail6"
    }
    if (!true.xor(false)) {
        return "fail7"
    }

    if (true.xor(true)) {
        return "fail8"
    }

    if (true.not()) {
        return "fail9"

    }

    if (!false.not()) {
        return "fail10"
    }

    return "OK"
}