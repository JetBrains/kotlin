package org.w3c.performance

public external interface GlobalPerformance {
    public abstract val performance: org.w3c.performance.Performance
        public abstract fun <get-performance>(): org.w3c.performance.Performance
}

public abstract external class Performance : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor Performance()
    public open val navigation: org.w3c.performance.PerformanceNavigation
        public open fun <get-navigation>(): org.w3c.performance.PerformanceNavigation
    public open val timing: org.w3c.performance.PerformanceTiming
        public open fun <get-timing>(): org.w3c.performance.PerformanceTiming
    public final fun now(): kotlin.Double
}

public abstract external class PerformanceNavigation {
    /*primary*/ public constructor PerformanceNavigation()
    public open val redirectCount: kotlin.Short
        public open fun <get-redirectCount>(): kotlin.Short
    public open val type: kotlin.Short
        public open fun <get-type>(): kotlin.Short

    public companion object Companion {
        public final val TYPE_BACK_FORWARD: kotlin.Short
            public final fun <get-TYPE_BACK_FORWARD>(): kotlin.Short
        public final val TYPE_NAVIGATE: kotlin.Short
            public final fun <get-TYPE_NAVIGATE>(): kotlin.Short
        public final val TYPE_RELOAD: kotlin.Short
            public final fun <get-TYPE_RELOAD>(): kotlin.Short
        public final val TYPE_RESERVED: kotlin.Short
            public final fun <get-TYPE_RESERVED>(): kotlin.Short
    }
}

public abstract external class PerformanceTiming {
    /*primary*/ public constructor PerformanceTiming()
    public open val connectEnd: kotlin.Number
        public open fun <get-connectEnd>(): kotlin.Number
    public open val connectStart: kotlin.Number
        public open fun <get-connectStart>(): kotlin.Number
    public open val domComplete: kotlin.Number
        public open fun <get-domComplete>(): kotlin.Number
    public open val domContentLoadedEventEnd: kotlin.Number
        public open fun <get-domContentLoadedEventEnd>(): kotlin.Number
    public open val domContentLoadedEventStart: kotlin.Number
        public open fun <get-domContentLoadedEventStart>(): kotlin.Number
    public open val domInteractive: kotlin.Number
        public open fun <get-domInteractive>(): kotlin.Number
    public open val domLoading: kotlin.Number
        public open fun <get-domLoading>(): kotlin.Number
    public open val domainLookupEnd: kotlin.Number
        public open fun <get-domainLookupEnd>(): kotlin.Number
    public open val domainLookupStart: kotlin.Number
        public open fun <get-domainLookupStart>(): kotlin.Number
    public open val fetchStart: kotlin.Number
        public open fun <get-fetchStart>(): kotlin.Number
    public open val loadEventEnd: kotlin.Number
        public open fun <get-loadEventEnd>(): kotlin.Number
    public open val loadEventStart: kotlin.Number
        public open fun <get-loadEventStart>(): kotlin.Number
    public open val navigationStart: kotlin.Number
        public open fun <get-navigationStart>(): kotlin.Number
    public open val redirectEnd: kotlin.Number
        public open fun <get-redirectEnd>(): kotlin.Number
    public open val redirectStart: kotlin.Number
        public open fun <get-redirectStart>(): kotlin.Number
    public open val requestStart: kotlin.Number
        public open fun <get-requestStart>(): kotlin.Number
    public open val responseEnd: kotlin.Number
        public open fun <get-responseEnd>(): kotlin.Number
    public open val responseStart: kotlin.Number
        public open fun <get-responseStart>(): kotlin.Number
    public open val secureConnectionStart: kotlin.Number
        public open fun <get-secureConnectionStart>(): kotlin.Number
    public open val unloadEventEnd: kotlin.Number
        public open fun <get-unloadEventEnd>(): kotlin.Number
    public open val unloadEventStart: kotlin.Number
        public open fun <get-unloadEventStart>(): kotlin.Number
}