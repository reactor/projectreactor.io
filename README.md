# Project Reactor Website

This is the source code for the Project Reactor home page. It also serves as an example application of how to use Ratpack, Reactor, and other web technolgies in a single project.

It's a normal Spring Boot project, so if you don't plan on hacking the SASS or CoffeeScript, you can just run the application like any other Spring Boot project:

		$ ./gradlew bootRun

If you plan on changing the SASS CSS or CoffeeScript, you'll need to compile those artifacts into the project using `grunt`:

		$ grunt watch

This will just compile the CSS and JavaScript into the src/main/resources/public directory of the Spring Boot app. You'll still have to build the app and deploy it using the standard Spring Boot deployment mechanism. There is a grunt task to do this in one step:

		$ grunt build

This project is Apache 2.0 licensed, just like the rest of Reactor.