## Build the example project 
1. Run in the root of the kotlin project

```shell
./gradlew install
```

2. Run example in the browser
```shell
./gradlew browserDevelopmentRun -t
```

## Troubleshooting

If you don't see that changes in toolchain is not applied to example project, try: 

1. Try to stop gradle daemons
```shell
./gradlew --stop
```

2. Try to build without gradle's build cache using `--no-build-cache` option 

3. Remove ~/.m2/org/jetbrains/kotlin and run "install" task again  
