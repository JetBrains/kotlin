package test;

public abstract class C extends B implements I {
    private final int mySomething1;
    private int mySomething6;

    C(int something1) {
        mySomething1 = something1;
    }

    @Override
    public int getSomething1() {
        return mySomething1;
    }

    @Override
    public int getSomething2() {
        return 0;
    }

    @Override
    public int getSomething3() {
        return 0;
    }

    @Override
    public void setSomething3(int value) {
    }

    @Override
    public int getSomething4() {
        return 0;
    }

    @Override
    public void setSomething5(int value) {

    }

    public int getSomething6() {
        return mySomething6;
    }

    @Override
    public void setSomething6(int value) {
        mySomething6 = value;
    }

    public String getFromB5() {
        return "";
    }

    @Override
    public void setFromB5(String value) {
        super.setFromB5(value);
    }
}
