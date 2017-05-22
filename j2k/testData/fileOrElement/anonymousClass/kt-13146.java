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

public class Handler {
    public void postDelayed(Runnable r, long time) {}
}

public class Test3 {
    private Handler handler = new Handler();

    private Runnable someRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(someRunnable, 1000);
        }
    };
}