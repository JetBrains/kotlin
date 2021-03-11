public class InnerClassWithoutName {
    public void uses() {
        Test5 test5 = new Test5();
        Test5.In1 in1 = test5.new In1();
    }
}