abstract class C implements B {
    @Override
    public String <caret>getFoo() {
        return "C";
    }

    @Override
    public void setFoo(String value) {

    }
}