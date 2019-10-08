class Test {
    public static void main(String[] args) {
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        int e = 0;
        //-----
        int f = a = b += c = d *= e;
        //-----
        int g = a;
        //-----
        int h = a = b;
        //-----
        int i = a += b;
        //-----
        int j = a += b = c;
    }
}