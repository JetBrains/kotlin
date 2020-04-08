interface <lineMarker descr="*">SkipSupport</lineMarker> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl1<br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>">skip</lineMarker>()
}

public interface <lineMarker descr="*">SkipSupportWithDefaults</lineMarker> : SkipSupport {
    override fun <lineMarker descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl1</body></html>"><lineMarker descr="Implements function in 'SkipSupport'">skip</lineMarker></lineMarker>() {}
}

public interface SkipSupportImpl1 : SkipSupportWithDefaults {
    override fun <lineMarker descr="Overrides function in 'SkipSupportWithDefaults'">skip</lineMarker>() {}
}

open class SkipSupportImpl : SkipSupportWithDefaults