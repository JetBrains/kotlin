package cats;

/**
 * Java code that uses Lombok-generated methods on {@link Cat},
 * and calls back into Kotlin via {@link CatClinic}.
 */
public class JavaUsage {

    public static void main(String[] args) {
        Cat cat = new Cat();
        cat.setName("Whiskers");
        cat.setLives(9);
        cat.setPurring(true);
        System.out.println(cat);
    }

    public static void crossLanguageCall() {
        new CatClinic().checkup();
    }
}