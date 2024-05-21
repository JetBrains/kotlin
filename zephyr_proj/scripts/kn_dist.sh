#!/bin/bash
cd /Users/txie/kn_exp
chmod +w /Users/txie/kn_exp/kotlin-native/dist/konan/lib/trove4j.jar
./gradlew :kotlin-native:zephyr_m55crossDist
