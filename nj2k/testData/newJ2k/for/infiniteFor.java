//method
boolean stop() {
    return false;
}
void foo() {
    for(;;) {
        if (!stop()) break;
    }
}