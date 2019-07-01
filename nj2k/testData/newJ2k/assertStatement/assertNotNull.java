abstract class C {
    void foo() {
        String s1 = f();
        assert s1 != null;

        String s2 = g();
        assert s2 != null : "g should not return null";
        int h = s2.hashCode();
    }

    abstract String f();
    abstract String g();
}