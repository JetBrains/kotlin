public class EnumJavaRef {
    public void use(EnumTarget p) {
        System.out.println(p == EnumTarget./*rename*/RED);
    }
}