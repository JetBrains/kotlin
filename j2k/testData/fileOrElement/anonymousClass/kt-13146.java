public class Test {
    public Runnable someRunnable = new Runnable() {
        @Override
        public void run() {
            someRunnable.run();
        }
    };
}

public class Test2 {
    private Runnable someRunnable = new Runnable() {
        @Override
        public void run() {
            someRunnable.run();
        }
    };
}
