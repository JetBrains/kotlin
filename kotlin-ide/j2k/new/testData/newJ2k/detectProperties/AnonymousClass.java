public class X {
    void foo() {
        Runnable runnable = new Runnable() {
            int f = 10;

            int getValue() { return f; }

            @Override
            public void run() {
                System.out.println(getValue());
            }
        };
    }
}
