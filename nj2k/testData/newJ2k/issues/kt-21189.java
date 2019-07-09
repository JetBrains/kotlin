public class ArrayInitializerBugKt {

    private static final byte[] GREETING = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'b', 'u', 'g', '!' };

    public static void main(String... args) {
        String greeting = new String(GREETING);
        System.out.println(greeting);
    }

}