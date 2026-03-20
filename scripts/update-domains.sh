#!/bin/bash
UPDATE_DOMAINS_DUMP=true ./gradlew :gradle-build-conventions:test-federation-convention:test --tests "org.jetbrains.kotlin.testFederation.DomainsDumpTest" --rerun
