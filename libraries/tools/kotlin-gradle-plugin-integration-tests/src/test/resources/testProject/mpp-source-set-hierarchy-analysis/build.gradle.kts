plugins {
	kotlin("multiplatform")
}

repositories {
	mavenLocal()
	mavenCentral()
}

kotlin {
    jvm { }
    js { 
        nodejs { }
    }

    linuxX64 { }
    linuxArm64 { }
}

// customized content below