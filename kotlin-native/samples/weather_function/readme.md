# Weather Function Sample

This is a Serverless Function that fetches the current weather information from the Open Weather Map [service](https://openweathermap.org/current) via HTTP. Part of the Function's output includes HTTP response headers, and the HTTP response body. This sample is designed to be deployed on OpenFaaS via a Docker image, in conjunction with Docker Swarm.


# Initial Setup

If the **~/.konan/cache** directory doesn't exist then you will need to compile a basic Linux amd64 program with konan before proceeding.

1. Clone the Kotlin Native Git repository: `git clone https://github.com/JetBrains/kotlin-native ~/repos/kotlin-native`
2. Change working directory to the sample: `cd ~/repos/kotlin-native/samples/weather_function`
3. Copy the Konan cache to the sample: `mkdir -p function/.konan || cp -R ~/.konan/cache function/.konan`
4. Copy the Gradle Wrapper to the sample: `cp ../gradle function`
5. Copy the **gradlew** file to the sample: `cp ../gradlew function`
6. Create the **openweathermap_key.txt** file in the **function** directory: `touch function/openweathermap_key.txt`
7. Append your [Open Weather Map API key](https://openweathermap.org/appid) to **function/openweathermap_key.txt**


# Building Docker Image

It will be assumed that [Docker](https://store.docker.com/search?type=edition&offering=community), and [OpenFaaS](https://docs.openfaas.com/deployment/) is already installed along with the [OpenFaaS CLI](https://github.com/openfaas/faas-cli). Build the *weather* Docker image using the **fass-cli** tool: `faas-cli build -f weather.yml`


# Usage

Make sure that you have completed the *Building Docker Image* section before proceeding.

1. Start OpenFaaS
2. Deploy the **weather** image using the **faas-cli** tool: `faas-cli deploy --image weather --name weather`
3. Invoke the **weather** function (including passing through the location argument) by running the following: `echo '-l="christchurch,nz"' | faas-cli invoke weather`

**Note:** The program (weather) can print weather information from a JSON file, eg: `./weather -f="current_weather.json"`. This functionality isn't available in the Serverless Function unless the file is generated in the **~/repos/kotlin-native/samples/weather_function/function** directory, the Dockerfile is updated to include the file in the Docker image, and the image is built (refer to the *Building Docker Image* section) before deploying the image (refer to the *Usage* section).
