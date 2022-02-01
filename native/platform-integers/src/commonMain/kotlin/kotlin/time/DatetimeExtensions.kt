@file:Suppress("NO_ACTUAL_FOR_EXPECT", "PHANTOM_CLASSIFIER", "LEAKING_PHANTOM_TYPE")

package kotlin.time

@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.days", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.days: Duration
@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.hours", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.hours: Duration
@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.minutes", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.minutes: Duration
@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.seconds", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.seconds: Duration
@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.milliseconds", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.milliseconds: Duration
@Deprecated(
    "Use extension property from Duration.Companion instead.",
    ReplaceWith("this.microseconds", "kotlin.time.Duration.Companion.days")
)
expect val PlatformInt.microseconds: Duration

expect fun PlatformInt.toDuration(unit: DurationUnit): Duration
