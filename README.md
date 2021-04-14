# Project Reactor Website

This is the source code for the Project Reactor home page. It also serves as an example
application of how to use Spring Web Reactive Framework, Reactor, and other web
technologies in a single project.

You can launch the application with by running:

		$ ./gradlew run

To enable live reload of static resources, including `.scss` files, run this command
when your application is still running:
 
		$ ./gradlew processResources -t

To package the application, including the Javadoc:
		
		$ ./gradlew clean build shadowJar

Make sure you have at least IntelliJ IDEA 2016.2.5 and Kotlin plugin 1.1-M02 to have
`build.gradle.kts` auto-complete and validation working correctly. You may have to
configure the EAP update site in:
Tools -> Kotlin -> Configure Kotlin Plugin Updates -> Early Access Preview 1.1

This project is Apache 2.0 licensed, just like the rest of Reactor.