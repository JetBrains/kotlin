import test.RemoveTopLevelPropertyReceiverBeforeKt;

class Test {
    static void test() {
        RemoveTopLevelPropertyReceiverBeforeKt.getP(new A());
        RemoveTopLevelPropertyReceiverBeforeKt.setP(new A(), 1);
    }
}