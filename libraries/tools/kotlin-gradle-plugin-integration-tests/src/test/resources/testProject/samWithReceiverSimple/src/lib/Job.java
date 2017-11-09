package lib;

@SamWithReceiver
public interface Job {
    public void execute(String context, int other);
}