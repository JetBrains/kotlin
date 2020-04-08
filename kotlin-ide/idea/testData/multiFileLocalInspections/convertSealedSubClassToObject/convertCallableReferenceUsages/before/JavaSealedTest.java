import seal.*

class JavaSealedTest {
    public void testNesting() {
        new SubSealed.Nested();
        Supplier<SubSealed.Nested> nestedSupplier = SubSealed.Nested::new;

        new SubSealed().internalFunction();
        Runnable noArgFunction = new SubSealed()::internalFunction;
    }
}