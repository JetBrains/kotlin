public class Base {
    protected int myX = 42;

    public int getX() {
        return myX;
    }

    Base(int x){
        myX = x;
    }
}

class Derived extends Base {
    Derived(Base b) {
        super(b.myX);
    }
}
