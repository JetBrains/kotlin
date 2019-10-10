package test;

public class UserImpl implements IUser {
    private final String name;

    public UserImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}