class A {
    // Comment for field1
    private int field1 = 0;
    private int field2; // comment for field2

    // comment before field3
    private int field3; // comment for field3

    public A(int field2) {
        this.field2 = field2;
    }

    // Comment for field1 getter
    public int getField1() {
        return field1;
    }

    /**
     * Comment for field1 setter
     */
    public void setField1(int field1) {
        this.field1 = field1;
    }

    // comment for field2 getter
    public int getField2() {
        return field2;
    }

    // comment for field2 setter
    public void setField2(int field2) {
        this.field2 = field2;
    }

    public int getField3() { return field3; } // comment for field3 getter
    public void setField3(int field3) { this.field3 = field3; } // comment for field3 setter

    // comment for getProperty
    public int getProperty() {
        return 1;
    } // end of getProperty

    // comment for setProperty
    public void setProperty(int value) {
    } // end of setProperty
}