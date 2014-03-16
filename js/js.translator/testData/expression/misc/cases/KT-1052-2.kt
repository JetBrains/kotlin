package foo

fun box(): Boolean {

    if (!true.and(true)) {
        return false;
    }
    if (false.and(true)) {
        return false;
    }

    if (!true.or(false)) {
        return false;
    }


    if (false.or(false)) {
        return false;
    }


    if (!true.or(true)) {
        return false;
    }


    if (false.and(false)) {
        return false;
    }

    if (false.xor(false)) {
        return false;
    }
    if (!true.xor(false)) {
        return false;
    }

    if (true.xor(true)) {
        return false;
    }

    if (true.not()) {
        return false;

    }

    if (!false.not()) {
        return false;
    }

    return true;
}