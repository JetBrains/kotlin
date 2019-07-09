public class TestClass {
    public static void main(String[] args) {
        for (int i = 1; i < 1000; i *= 2) {
            if (i == 4 || i == 8) continue;
            System.err.println(i);
        }
    }
}