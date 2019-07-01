
public class Test {
    public void someMethod() {
        Runnable someRunnable = new Runnable() {
            @Override
            public void run() {
                someRunnable.run();
            }
        };
    }
}