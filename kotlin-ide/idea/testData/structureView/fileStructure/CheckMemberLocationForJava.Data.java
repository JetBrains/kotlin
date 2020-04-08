public class JavaClass {
    public interface First {
        void foo123();
        void foo12_();
        void foo1_3();
        void foo_23();

        void foo1__();
        void foo_2_();
        void foo__3();

        void foo___();
    }

    public static abstract class A1 implements First {
        @Override public void foo123() {}
        @Override public void foo12_() {}
        @Override public void foo1_3() {}

        @Override public void foo1__() {}
    }

    public static abstract class A2 extends A1 {
        @Override public void foo123() {}
        @Override public void foo12_() {}
        @Override public void foo_23() {}

        @Override public void foo_2_() {}
    }

    public static class A3 extends A2 {
        @Override public void foo123() {}
        @Override public void foo1_3() {}
        @Override public void foo_23() {}

        @Override public void foo__3() {}

        // @Override public void foo___() {} // Removed intentionally
    }
}
