package foo


fun box(): Boolean {

    if (1 in -2..0) return false;
    if (1 in -10..-4) return false;
    if (!(1 in 0..2)) return false;

    if (!(1 in 1..2)) return false;
    if (!(1 in -2..5)) return false;

    return true;
}
