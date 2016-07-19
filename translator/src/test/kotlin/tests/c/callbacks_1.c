
int apply_c(int x, int (*foo)(int)) {
    return foo(x);
}
