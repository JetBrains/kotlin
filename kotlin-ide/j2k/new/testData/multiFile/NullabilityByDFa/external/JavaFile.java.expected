
public class SomeService {

    public static SomeService getInstanceNotNull() {
        return new SomeService();
    }

    public static SomeService getInstanceNullable() {
        if (Math.random() > 0.5)
            return null;
        return new SomeService();
    }

    public String nullableString() {
        if (Math.random() < 0.5)
            return null;
        return Math.random() + "";
    }

    public String notNullString() {
        String s = nullableString();
        if (s != null)
            return s;
        return "null";
    }
}