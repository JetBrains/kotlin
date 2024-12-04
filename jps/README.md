# How to publish JPS locally to consume it in Intellij Idea

```shell
../gradlew -p ./.. -Ppublish.ide.plugin.dependencies=true install && \
../gradlew -p ./.. -Ppublish.ide.plugin.dependencies=true installIdeArtifacts  && \
../gradlew -p ./.. -Ppublish.ide.plugin.dependencies=true installJps && \
echo Finished successfully
```