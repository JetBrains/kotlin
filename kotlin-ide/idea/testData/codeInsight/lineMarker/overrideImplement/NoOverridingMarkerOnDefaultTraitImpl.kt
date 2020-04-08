interface <lineMarker descr="*">SkipSupport</lineMarker> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>">skip</lineMarker>()
}

public interface <lineMarker descr="*">SkipSupportWithDefaults</lineMarker> : SkipSupport {
    override fun <lineMarker descr="Implements function in 'SkipSupport'">skip</lineMarker>() {}
}

open class SkipSupportImpl : SkipSupportWithDefaults