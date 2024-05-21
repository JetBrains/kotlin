# Kotlin Native x Zephyr Experimentation

## Build Sequence

1. Run the KN build to generate api and lib code, without invoking the `g++` step.
2. Run `Zephyr`'s `west` command to build the Zephyr firmware together with the `.o` file produced in step 1.
