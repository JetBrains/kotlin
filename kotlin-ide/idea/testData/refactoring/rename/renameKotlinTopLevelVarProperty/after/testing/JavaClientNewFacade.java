package testing;

import testing.rename.RenameTopLevelVarPropertyKt;

class JavaClientNewFacade {
    public void foo() {
        String old = RenameTopLevelVarPropertyKt.getBar();
        RenameTopLevelVarPropertyKt.setBar(old + "new");
    }
}