import javaApi.Base;

class C extends Base {
    public void f() {
        Base other = Base();
        int value = other.getProperty() + getProperty();
        other.setProperty(1);
        setProperty(other.getProperty() + value);
        getBase(getProperty()).setProperty(0);
    }

    private Base getBase(int i) {
        return new Base();
    }
}
