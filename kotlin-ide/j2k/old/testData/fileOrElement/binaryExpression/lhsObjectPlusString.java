public class Test {
    public static class B {
        public static class C {

        }

        public C c() {
            return new C();
        }
    }

    public void a() {
        B b = new B();
        System.out.println(b + "");
        String a = 1 + "0";
        System.out.println(b.c() + "");
    }

    public static void main(String[] args) {
        String p = new Test() + "123";
    }
}