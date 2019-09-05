class Test {
    public static void main(String[] args) {
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        int e = 0;

        a = b += c = d *= e;
        //-----
        a = b;
        //-----
        a += b;
        //-----
        a += b = c;
        //-----
        a = b += c;
    }
}