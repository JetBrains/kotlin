//file
interface I {
    String getString();
}

class C {
    void foo(I i) {
        String result = i.getString();
        if (result != null) {
            print(result);
        }
    }
}