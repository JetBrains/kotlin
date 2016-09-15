// FQNAME: EnumClass

// FILE: EnumClass.java
enum EnumClass {
    RED, GREEN, BLUE;

    void someMethod() {
        System.out.println("Hello, world!")
    }

    String getStringRepresentation() {
        return this.toString();
    }
}

// FILE: Anno.kt
annotation class Anno