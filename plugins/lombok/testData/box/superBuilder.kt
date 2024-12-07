// IGNORE_BACKEND_K1: ANY

// FILE: Vehicle.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Vehicle {
    private String make;
    private String model;
}

// FILE: Car.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Car extends Vehicle {
    private int numberOfDoors;
}

// FILE: test.kt

fun box(): String {
    val carBuilder = Car.builder()
        .numberOfDoors(4)
        .make("Fiesta")
        .model("Ford")

    val car = carBuilder.build()
    return if (car.numberOfDoors == 4 && car.make == "Fiesta" && car.model == "Ford") {
        "OK"
    } else {
        "Error: $car"
    }
}
