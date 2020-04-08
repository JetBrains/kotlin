package test;

@AnnTarget
public class UsageJava {
    @AnnTarget
    public UsageJava () {
        @AnnTarget int i = 10;
    }
    @AnnTarget
    public void apply(@AnnTarget String in) {}
}