public class TestJava {
    public static void main() {
        System.out.println((int) (Integer.MAX_VALUE + Integer.MAX_VALUE + 2.0));
        System.out.println((int) (Integer.valueOf(Integer.MAX_VALUE) + Integer.valueOf(Integer.MAX_VALUE) + 2.0));
    }
}