class A {
    private String s = "";
    private int x = 0;

    public A(){
    }

    public A(int p, String s){
        this(p, s, 1);
    }

    public A(int p, String s, int x){
        this.s = s;
        this.x = x;
    }
}