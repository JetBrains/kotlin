import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    compileOnly("com.android.tools.build:gradle:3.4.0")
}

pill {
    variant = PillExtension.Variant.FULL
}
