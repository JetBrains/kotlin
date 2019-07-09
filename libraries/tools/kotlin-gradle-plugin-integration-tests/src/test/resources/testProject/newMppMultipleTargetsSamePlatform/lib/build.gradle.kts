plugins {
	id("org.jetbrains.kotlin.multiplatform")
}

val testFrameworkAttribute = Attribute.of("com.example.testFramework", String::class.java)

kotlin {
	configure(listOf(jvm("junit"), jvm("testng"))) {
		attributes {
			attribute(testFrameworkAttribute, targetName)
		}
	}

	jvm("junit").compilations["main"].defaultSourceSet.dependencies {
		api("junit:junit:4.12")
	}

	jvm("testng").compilations["main"].defaultSourceSet.dependencies {
		api("org.testng:testng:6.14.3")
	}
}