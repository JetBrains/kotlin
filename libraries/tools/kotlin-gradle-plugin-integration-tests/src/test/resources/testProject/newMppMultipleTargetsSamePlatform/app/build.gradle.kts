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

	val commonMainSourceSet = sourceSets.commonMain.get()
	jvm("mixed") {
		configure(listOf(compilations.create("junit"), compilations.create("testng"))) {
			defaultSourceSet.dependsOn(commonMainSourceSet)
			attributes {
				attribute(testFrameworkAttribute, compilationName)
			}
		}
	}

	sourceSets["commonMain"].dependencies {
		implementation(project(":lib"))
	}
}