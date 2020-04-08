package test;

public class CommentUsage {
    public void context() {
        Used.usedVar2 = 0;
        Used.setUsedVar2(0);
        Used.getUsedVar2();
        Used.INSTANCE.usedVar2 = 0;
        Used.INSTANCE.setUsedVar2(0);
        Used.INSTANCE.getUsedVar2();
        //        Used.usedVar2 = 0;
        //        Used.setUsedVar2(0);
        //        Used.getUsedVar2();
        //        Used.INSTANCE.usedVar2 = 0;
        //        Used.INSTANCE.setUsedVar2(0);
        //        Used.INSTANCE.getUsedVar2();
        String v = "Used.usedVar2"
                   + "Used.setUsedVar2(0)"
                   + "Used.getUsedVar2()"
                   + "Used.INSTANCE.usedVar2"
                   + "Used.INSTANCE.setUsedVar2(0)"
                   + "Used.INSTANCE.getUsedVar2()";
    }
}