This module exists for the sole purpose of providing the classpath for the IDEA run configuration.
This run configuration takes a plugin from the KotlinPlugin artifact instead of the project's 'out' directory, which makes it possible for our plugin to depend on other plugins such as JUnit plugin.
If you want to debug some patch to IDEA, you can add copy of IDEA class into this module and modify.