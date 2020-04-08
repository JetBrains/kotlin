//file
class Outer {
    public static Object o = new Object();

    public static class Nested {
        public void foo() {
            o = null;
        }
    }
}
