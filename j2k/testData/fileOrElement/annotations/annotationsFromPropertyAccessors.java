@interface An {
    String value();
}


public class Test {
    private int id;

    @An(value = "get")
    public int getId() {
        return id;
    }

    @An(value = "set")
    public void setId(int id) {
        this.id = id
    }
}
