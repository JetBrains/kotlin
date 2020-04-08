interface I {
    int getX();
    void setX(int x);
}

class A implements I {
    private int x;

    public A(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setX(int x){
        this.x = x;
    }
}