/*∆*/ public external interface GlobalPerformance {
/*∆*/     public abstract val performance: org.w3c.performance.Performance { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class Performance : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor Performance()
/*∆*/ 
/*∆*/     public open val navigation: org.w3c.performance.PerformanceNavigation { get; }
/*∆*/ 
/*∆*/     public open val timing: org.w3c.performance.PerformanceTiming { get; }
/*∆*/ 
/*∆*/     public final fun now(): kotlin.Double
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class PerformanceNavigation {
/*∆*/     public constructor PerformanceNavigation()
/*∆*/ 
/*∆*/     public open val redirectCount: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public companion object of PerformanceNavigation {
/*∆*/         public final val TYPE_BACK_FORWARD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TYPE_NAVIGATE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TYPE_RELOAD: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val TYPE_RESERVED: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class PerformanceTiming {
/*∆*/     public constructor PerformanceTiming()
/*∆*/ 
/*∆*/     public open val connectEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val connectStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domComplete: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domContentLoadedEventEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domContentLoadedEventStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domInteractive: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domLoading: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domainLookupEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val domainLookupStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val fetchStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val loadEventEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val loadEventStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val navigationStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val redirectEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val redirectStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val requestStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val responseEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val responseStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val secureConnectionStart: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val unloadEventEnd: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val unloadEventStart: kotlin.Number { get; }
/*∆*/ }