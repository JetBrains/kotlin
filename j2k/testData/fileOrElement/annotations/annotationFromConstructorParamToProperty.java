@interface A {

}

@interface B {

}

public class U {
    @B
    public int i;

    public U(@A int i) {
        this.i = i;
    }
}