

public class JavaTest {
    public interface SAMInterface {
        void onEvent(int event);
    }

    public static class SomeJavaClass {
        public void setListener(SAMInterface listener) {}
    }
}
