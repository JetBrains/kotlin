public class Testing {
    public static void test() {
        DefaultImpl<caret>
    }
}

// EXIST:  { lookupString: "DefaultImpls", tailText: " defaultImpls.NonAbstractFun" }
// EXIST:  { lookupString: "DefaultImpls", tailText: " defaultImpls.NonAbstractFunWithExpressionBody" }
// EXIST:  { lookupString: "DefaultImpls", tailText: " defaultImpls.NonAbstractProperty" }
// EXIST:  { lookupString: "DefaultImpls", tailText: " defaultImpls.NonAbstractPropertyWithBody" }
// ABSENT:  { lookupString: "DefaultImpls", tailText: " defaultImpls.AllAbstract" }