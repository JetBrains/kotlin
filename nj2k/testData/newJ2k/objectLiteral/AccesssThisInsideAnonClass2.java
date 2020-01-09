public class Foo {

    void fail() {
        new Test.FrameCallback() {
            public void doFrame() {
                new Test().postFrameCallbackDelayed(this);
            }
        };
    }
}

class Test {
    public interface FrameCallback {
        void doFrame();
    }

    void postFrameCallbackDelayed(Test.FrameCallback callback) {
    }
}