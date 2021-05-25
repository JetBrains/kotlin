import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    api(project(":native:kotlin-native-utils"))
    api(project(":kotlin-project-model"))

    compileOnly("com.android.tools.build:gradle:3.4.0")
}

pill {
    variant = PillExtension.Variant.FULL
}
