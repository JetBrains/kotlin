package dagger_example;

import javax.inject.Inject;

public class Main {
    @Inject
    Injected injected;

    @Inject
    OtherInjected otherInjected;

    public static void main(String[] args) {
        new Main().example();
    }

    private void example() {
        DaggerApplicationComponent.create().inject(this);
        System.out.println(injected.getMessage());
    }
}
