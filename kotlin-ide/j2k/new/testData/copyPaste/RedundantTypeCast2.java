class C {
    void foo(Object o) {
<selection>        if (!(o instanceof String)) return
        int l = ((String) o).length();
</selection>    }
}