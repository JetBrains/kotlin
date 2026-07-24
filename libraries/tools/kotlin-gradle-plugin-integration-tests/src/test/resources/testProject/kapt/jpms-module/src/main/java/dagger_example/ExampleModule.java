package dagger_example;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class ExampleModule {
    @Binds
    public abstract Injected bindInjected(InjectedImpl impl);

    @Binds
    public abstract OtherInjected bindOtherInjected(OtherInjectedImpl impl);
}
