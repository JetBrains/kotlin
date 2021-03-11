public class TestInitializeInTry {
    Object x;
    private Object y;

    public TestInitializeInTry() {
        try {
            x = new Object();
            y = new Object();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        x.toString();
        y.toString();
    }
}