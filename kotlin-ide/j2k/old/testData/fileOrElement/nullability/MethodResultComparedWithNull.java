//file
interface I {
    String getString();
}

class C {
    void foo(I i) {
        if (i.getString() == null) {
            println("null");
        }
    }
}