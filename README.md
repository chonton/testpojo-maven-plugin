# testpojo-maven-plugin
Test pojos using maven plugin instead of boilerplate.  Just as [lombok](https://projectlombok.org/) can reduce writing bolierplate for POJOs, this plugin can reduce the unit tests you need to write.

There is one goal: test which defaults to running in the test phase.  Using [Reflections](https://github.com/ronmamo/reflections), all code in the ${build.build.outputDirectory} is introspected.  Any class with method implementations for both equals and hashCode is considered a Bean.

The Bean is tested by first constructing with no args public constructor, using [Jackson](https://github.com/FasterXML/jackson) to marshall to Map and back to new instance of POJO.  The original bean and its copy are then compared and the hashCodes are compared.
The Bean is further tested by using [random beans](https://github.com/benas/random-beans) to populate the bean and again marshalls, demarshalls, and compares original to copy.
If the Bean uses Lombok's @Builder, the builder will be used to populate the bean.
