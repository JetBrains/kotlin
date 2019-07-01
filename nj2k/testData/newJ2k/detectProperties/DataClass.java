public class Test {
    private String id;
    private String name;
    private int myAge;

    public Test(String id, String name, int anAge) {
        this.id = id;
        this.name = name;
        myAge = anAge;
        System.out.println(anAge);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return myAge;
    }
}
