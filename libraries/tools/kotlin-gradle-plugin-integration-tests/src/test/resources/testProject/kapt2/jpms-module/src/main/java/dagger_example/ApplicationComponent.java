package dagger_example;

import dagger.Component;

@Component(modules = {ExampleModule.class})
public interface ApplicationComponent {
    void inject(Main main);
}
