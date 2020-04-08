public class TestClass {
    public static void main(String[] args) {
        for (int i = 0, j = 1; i < 10; ++i, j *= 2) {
            if (i == 4 || i == 8) {
                i++;
                continue;
            }
            System.err.println(j);
        }
    }
}