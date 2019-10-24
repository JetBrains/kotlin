
public class SomeServiceUsage {

    public SomeService getService() {
        return SomeService.getInstanceNotNull();
    }

    public SomeService getServiceNullable() {
        return SomeService.getInstanceNullable();
    }

    // elvis
    public SomeService getServiceNotNullByDataFlow() {
        SomeService s = SomeService.getInstanceNullable();
        return s == null ? SomeService.getInstanceNotNull() : s;
    }

    // nullable, bang-bang
    public String aString1() {
        return getServiceNullable().nullableString();
    }

    // nullable
    public String aString2() {
        return getService().nullableString();
    }

    // not nullable
    public String aString3() {
        return getService().notNullString();
    }

    // nullable, no bang-bang
    public String aString4() {
        return getServiceNotNullByDataFlow().nullableString();
    }

    // not nullable, no bang-bang
    public String aString5() {
        return getServiceNotNullByDataFlow().notNullString();
    }

    // nullable, safe-call
    public String aString6() {
        SomeService s = getServiceNullable();
        if (s != null) {
            return s.nullableString();
        } else {
            return null;
        }
    }

}