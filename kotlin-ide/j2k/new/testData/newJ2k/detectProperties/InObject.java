public class AAA {
    private static int ourX = 42;
    private static int y = 0;
    private static int z = 0;

    public static int getX() {
        return ourX;
    }

    public static void setX(int x) {
        ourX = x;
    }

    public static int getY() {
        return y;
    }

    public static void setY(int y) {
        AAA.y = y;
    }

    public static int getZ() {
        return z;
    }

    public static void setZ(int z) {
        Other.z = z;
    }
}

class Other {
    public static int z = 0;
}
