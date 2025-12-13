# AB Testing Demo Project

This project is a reference implementation and demonstration of [Proctor](https://github.com/indeedeng/proctor), a Java-based A/B testing framework by [Indeed](http://engineering.indeed.com). It is a simple Spring MVC webapp that uses Proctor to determine what background color to set for a web application.

For more information, see the [Proctor documentation](http://indeedeng.github.io/proctor). The [Quick Start](http://indeedeng.github.io/proctor/docs/quick-start/) document is a good guide to understanding the code in this reference implementation.

## Demo Online

The demo is running [here]( https://abtesting-79wy.onrender.com/). It loads its [test definitions](http://indeedeng.github.io/proctor/docs/terminology/#toc_4) from JSON files that can be at any URL. 
Right now it is using [this](https://gist.github.com/mesutdurukal/343755729c7ddedee28b49f3c22d7917/) json. You can override it with the `?defn=<url>` query parameter.

### Things to Try

1. Testing color variants like 50% and 50% or 100% for one color.

1. Using `prforceGroups` to see a different test group, regardless of allocations: [link](https://abtesting-79wy.onrender.com/?prforceGroups=bgcolortst3)

1. Basing color on Android vs. iOS user agent instead of random allocation: [link](https://abtesting-79wy.onrender.com/?mydevice) which indeed goes to [link](https://abtesting-79wy.onrender.com/?defn=https://gist.githubusercontent.com/mesutdurukal/bd424fa4cfc069010882791004beb9d8/raw). If you're not on Android or iOS you won't see a background color.

## Building and Running Demo Locally

1. `mvn clean package && java -jar target/dependency/webapp-runner.jar target/*.war`

1. Go to [http://localhost:8080/](http://localhost:8080/)

To run on a different port locally, use the `--port` option for webapp-runner. For example, to run on port 9999:
```
java -jar target/dependency/webapp-runner.jar --port 9999 target/*.war
```

## The Source

### [ProctorGroups.json](https://github.com/indeedeng/proctor-demo/blob/master/src/main/proctor/com/indeed/demo/ProctorGroups.json)
The JSON specification that is enumerates the test and its buckets. This is used to generate convenience classes at compile time and to load the test matrix at runtime.

### [DefinitionManager.java](https://github.com/indeedeng/proctor-demo/blob/master/src/main/java/com/indeed/demo/proctor/DefinitionManager.java)
Helper component that manages loading and caching the test matrix from a definition file at a remote URL.

### [DemoController.java](https://github.com/indeedeng/proctor-demo/blob/master/src/main/java/com/indeed/demo/proctor/DemoController.java)
Spring controller that handles assigning a UUID cookie to identify the user and calling into proctor to get the groups for the current user. Also provides `/rpc` service endpoint support.

### [demo.jsp](https://github.com/indeedeng/proctor-demo/blob/master/src/main/webapp/WEB-INF/jsp/demo.jsp)
Java Servlet Page view for the demo controller; renders the test behavior and some controls to interact with the demo.

### [UserAgent.java](https://github.com/indeedeng/proctor-demo/blob/master/src/main/java/com/indeed/web/useragents/UserAgent.java)
A helper class based partially on bitwalker's UserAgentUtils that can be a useful context parameter for proctor.
