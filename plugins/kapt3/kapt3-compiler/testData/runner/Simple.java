package test;

/**
 * KDoc comment.
 */
class Simple {
    @MyAnnotation
    void myMethod() {
        // do nothing
    }
}

@interface MyAnnotation {

}

enum EnumClass {
    BLACK, WHITE
}


enum EnumClass2 {
    WHITE("A"), RED("B");

    private final String blah;

    EnumClass2(String blah) {
        this.blah = blah;
    }
}

enum EnumClass3 {
    A {
        @Override
        void a() {}
    };

    abstract void a();
}