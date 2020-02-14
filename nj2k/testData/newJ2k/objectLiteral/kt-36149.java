// Stubbed from Android Activity
class Activity {
    public final void runOnUiThread(Runnable action) {
        action.run();
    }
}

public class Foo {
    private Activity activity;

    public void foo() {
        synchronized (this) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                }
            });
        }
    }

    public void bar() {
        synchronized (this) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                }
            });
        }
    }
}