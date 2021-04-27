import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    api(project(":native:kotlin-native-utils"))

    compileOnly("com.android.tools.build:gradle:3.4.0")
    compileOnly(project(":kotlin-project-model"))
}

pill {
    variant = PillExtension.Variant.FULL
}
