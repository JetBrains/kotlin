Updating version of gradle-api.jar

 * change version in build.gradle file

 * change gradle.api.jar.version in pom.xml

 * run command: gradlew wrapper

 * deploy gradle api to maven

Deploying gradle api to maven

 * gradlew build
   -> build/libs/gradle-api-1.6.jar
 
 * mvn deploy
   build/libs/gradle-api-1.6.jar -> http://repository.jetbrains.com/utils
