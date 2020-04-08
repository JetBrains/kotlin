public class TestToStringReturnsNullable {
    public static class Base {
        public String string;
    }

    public static class Ctor extends Base {
        public Ctor(String string) {
            this.string = string;
        }
    }

    public static class Derived extends Ctor {
        public Derived(String string) {
            super(string);
        }

        @Override
        public String toString() {
            return string;
        }
    }
}