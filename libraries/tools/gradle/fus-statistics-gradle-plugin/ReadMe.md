## Description

Contains a plugin for Feature Usage Statistics(FUS). fus-statistics-gradle-plugin can be used by other Gradle plugins to
collect additional metrics for FUS.  

Statistics will be gathered in **kotlin-fus/<build_uid>** file. It is located in ${GRADLE_USER_HOME} directory by default. 
You can modify default directory by using the **kotlin.fus.statistics.path** property (for test purpose only). 
Created file will contain **key=value** rows fowled by **BUILD FINISHED** 

Collection statistics in a file is enabled by default. Further step is to collect data only after approval - KT-59629. 
In any case, collected data will be sent only after user's agreement.  

### Collect own data

**org.jetbrains.kotlin.fus-statistics-gradle-plugin** plugin should be applied and a new **UsesGradleBuildFusStatisticsService** task
should be created to be able to collect additional data. 
In this case, the task will access **GradleBuildFusStatistics** and be able to report new metrics via **reportMetric**.

Analytics teams' approve is required for any collected data.
