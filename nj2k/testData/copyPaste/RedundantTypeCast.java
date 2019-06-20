class C {
    void foo(Object o) {
<selection>        if (o instanceof String) {
            int l = ((String) o).length();
        }
</selection>    }
}