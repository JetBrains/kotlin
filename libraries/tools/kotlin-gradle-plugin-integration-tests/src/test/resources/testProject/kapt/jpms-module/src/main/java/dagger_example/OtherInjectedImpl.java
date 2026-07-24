package dagger_example;

import javax.inject.Inject;

public class OtherInjectedImpl implements OtherInjected {

    @Inject
    public OtherInjectedImpl() {

    }

    @Override
    public String getMessage() {
        return "zzzz";
    }
}
