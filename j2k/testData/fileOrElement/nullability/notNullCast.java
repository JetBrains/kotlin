public class Passenger {
    public static class PassParent {
    }

    public static class PassChild extends PassParent {
    }

    public PassParent provideNullable(int p) {
        return p > 0 ? new PassChild() : null;
    }

    public void test1() {
        PassParent pass = provideNullable(1);
        assert pass != null;
        accept1((PassChild) pass);
    }

    public void test2() {
        PassParent pass = provideNullable(1);
        if (1 == 2) {
            assert pass != null;
            accept2((PassChild) pass);
        }
        accept2((PassChild) pass);
    }

    public void accept1(PassChild p) {
    }

    public void accept2(PassChild p) {
    }
}