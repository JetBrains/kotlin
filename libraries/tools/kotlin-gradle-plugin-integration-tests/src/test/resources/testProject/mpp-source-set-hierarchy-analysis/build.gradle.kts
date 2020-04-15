plugins {
	kotlin("multiplatform")
}

repositories {
	mavenLocal()
	jcenter()
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