public class J {
    public static void main(String[] args) {
        boolean a = false, b = true, c = false;
        c &= a || b;
        System.out.print(c);
    }
}