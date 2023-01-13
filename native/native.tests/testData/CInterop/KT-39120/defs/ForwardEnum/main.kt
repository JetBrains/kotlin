import pod1.*
import pod2.*

fun box(): String {
    if (pod1.varPOD != pod2.ForwardEnumPOD.Value2POD)
        return "OK"
    else
        return "FAIL"
}
