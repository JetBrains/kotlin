public class Foo {
    private Integer someInt;
    public void setState(Integer state) {
        //some comment 1
        someInt = state;
        //some comment 2
        if (state == 2)
            System.out.println("2");
    }
    public Integer getState() {
        return someInt;
    }
}