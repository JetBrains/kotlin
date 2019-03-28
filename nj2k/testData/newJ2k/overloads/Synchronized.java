class A {
    public void foo(int p) {
        System.out.println("p = [" + p + "]");
    }

    public synchronized void foo(){
        foo(calcSomething());
    }

    // this method should be invoked under synchronized block!
    private int calcSomething() { return 0; }
}