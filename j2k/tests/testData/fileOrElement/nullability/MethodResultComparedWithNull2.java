//file
interface I {
    String getString();
}

class C {
    void foo(I i) {
        final String result = i.getString();
        if (result != null) {
            print(result);
        }
    }
}