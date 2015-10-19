class C {
    void foo(Object o) {
        if (o instanceof String) {
            int l = ((String) o).length();
        }
    }
}