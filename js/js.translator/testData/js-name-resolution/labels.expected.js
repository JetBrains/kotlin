function foo() {
    x: {
        x_0: {
            console.log("1");
            console.log(function() {
                x: {
                    console.log("2");
                }
            });

            if (condition()) {
                break x;
            }
            else {
                break x_0;
            }
        }
    }

    x: {
        x_0: {
            console.log("1");
        }
        x_0: {
            console.log("1");
        }
    }
}