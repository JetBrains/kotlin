interface I {
    int getSomething1();

    int getSomething2();

    int getSomething3();
    void setSomething3(int value);

    int getSomething4();
    void setSomething4(int value);

    int getSomething5();
    void setSomething5(int value);

    void setSomething6(int value);
}

interface I1 extends I {
    void setSomething1(int value);

    int getSomething6();
}

class B {
    public String getFromB1() {
        return "";
    }

    public String getFromB2() {
        return "";
    }

    public void setFromB2(String value) {
    }

    public String getFromB3() {
        return "";
    }

    public void setFromB3(String value) {
    }

    public String getFromB4() {
        return "";
    }

    public void setFromB4(String value) {
    }

    public void setFromB5(String value) {
    }
}

abstract class C extends B implements I {
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

    @Override
    public String getFromB1() {
        return super.getFromB1();
    }

    @Override
    public String getFromB2() {
        return super.getFromB2();
    }

    @Override
    public void setFromB2(String value) {
        super.setFromB2(value);
    }

    @Override
    public String getFromB3() {
        return super.getFromB3();
    }

    @Override
    public void setFromB4(String value) {
        super.setFromB4(value);
    }

    public String getFromB5() {
        return "";
    }

    @Override
    public void setFromB5(String value) {
        super.setFromB5(value);
    }
}
