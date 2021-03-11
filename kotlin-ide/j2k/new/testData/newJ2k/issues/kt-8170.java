public class ArrayNullable {
    public static void main(String[] args) {
        int notNull = 0;

        Integer[] a1 = new Integer[2];
        a1[0] = null;
        a1[1] = notNull;
        System.out.println(a1[0]);
        System.out.println(a1[1]);

        Integer[] a2 = new Integer[2];
        a2[0] = nullable();
        a2[1] = notNull;
        System.out.println(a2[0]);
        System.out.println(a2[1]);

        Integer[] a3 = new Integer[] { null, nullable(), notNull };
        System.out.println(a3[0]);
        System.out.println(a3[1]);
        System.out.println(a3[2]);
    }

    public static Integer nullable() { return System.getProperty("user.home").length() > 20 ? null : 1; }
}