import test.AddTopLevelPropertyReceiverBeforeKt;

class Test {
    static void test() {
        AddTopLevelPropertyReceiverBeforeKt.getP(A());
        AddTopLevelPropertyReceiverBeforeKt.setP(A(), 1);
    }
}