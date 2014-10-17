public class C {
    private final int myX;

    public int getX() {
        return myX;
    }

    C(C c, int x){
        myX = x;
    }

    C(C c){
        this(c, c.myX);
    }
}
