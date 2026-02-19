public class Main {
    public static void staticMethod() {}
    public static String staticField = "";
    public void regularMethod() {}
    public String regularField = "";

    public void invoke() {
    }

    public static Main INSTANCE = new Main();

    public static class Other extends Main {
    }
}