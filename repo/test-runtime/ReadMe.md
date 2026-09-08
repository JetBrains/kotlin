# Test Runtime Module

This module contains classes which shall always be available on the classpath when running tests. 
The test configuration is expected to be done by the 'project-tests-convention' which enables auto-detection of JUnit extensions. 
This module may offer fundamental APIs to our repository (e.g. Test Federation APIs), but also JUnit extensions that are
universally necessary for our builds.
