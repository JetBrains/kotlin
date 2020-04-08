public class TestClass {
    public static void main(String[] args) {
        OuterLoop1:
        OuterLoop2:
        for (int i = 1; i < 1000; i *= 2) {
            InnerLoop:
            for (int j = 1; j < 100; j *= 3) {
                if (j == 3) continue InnerLoop;
                if (i == j) continue OuterLoop1;
                System.err.println(j);
                if (j == 9) continue;
            }
        }
    }
}