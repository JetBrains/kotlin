package testing;

import testing.rename.RenameTopLevelVarPropertyKt;

class JavaClientNewFacade {
    public void foo() {
        String old = RenameTopLevelVarPropertyKt.getFoo();
        RenameTopLevelVarPropertyKt.setFoo(old + "new");
    }
}