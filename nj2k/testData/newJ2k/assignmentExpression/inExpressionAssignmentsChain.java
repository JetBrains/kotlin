class Test {
    public static void main(String[] args) {
        boolean a = true;
        boolean b = true;
        boolean c = true;
        boolean d = true;
        boolean e = true;

        if (a = b |= c = d &= e) ;
        while (a = b) ;
        do {} while (a ^= b);
        System.out.println(a &= b = c);
        System.out.println(a = b != c);
    }
}