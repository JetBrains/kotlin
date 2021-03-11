package test.nullabilityOnClassBoundaries;

public class Item {
    private String s1;
    private String s2;

    public void set(String s1, String s2) {
        this.s1 = s1;
        this.s2 = s2;
    }
}

public class Reader {
    public Item readItem(int n) {
        Item item = new Item();
        item.set(readString(n), null);
        return item;
    }

    public String readString(int n) {
        if (n <= 0) return null;
        return Integer.toString(n);
    }
}