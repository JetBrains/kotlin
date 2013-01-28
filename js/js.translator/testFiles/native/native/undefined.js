function f(g) {
    if (g() != 0) {
        return false;
    }
    if (g(1) != 1) {
        return false;
    }
    if (g(2) != 3) {
        return false;
    }
    return true;
}