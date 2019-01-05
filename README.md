# testpojo-maven-plugin
Test pojos using maven plugin instead of boilerplate.  Just as [lombok](https://projectlombok.org/) can reduce writing bolierplate for POJOs, this plugin can reduce the unit tests you need to write.

## Requirements
This plugin is designed to used with [jacoco maven plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html).

## Maven Configuration
To include testpojo-maven-plugin in your maven build, use the following fragment in your pom.
``` xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>testpojo-maven-plugin</artifactId>
        <version>0.0.9</version>
        <executions>
          <execution>
            <id>test-pojos</id>
            <goals>
              <goal>test</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

testpojo-maven-plugin provides a single 'test' goal which defaults to running in the test phase. 

## What gets tested
* constructor
* equals
* hashCode
* getters and setters
* [lombok builder](https://projectlombok.org/features/Builder.html)
* [Jackson](https://github.com/FasterXML/jackson) marshalling and demarshalling

## How testpojo-maven-plugin works
Using [Reflections](https://github.com/ronmamo/reflections), all code in the ${build.outputDirectory} is introspected.  Any class with method implementations for both equals and hashCode and has a public constructor is considered a bean.

| Parameter | Default | Description |
|-----------|---------|-------------|
|testpojo.skip |${skipTests}| Skip testing pojos |

### Bean test consists of the following steps.
1. Construct bean with public constructor having least number of arguments.
2. Execute toString() and make sure no exceptions occur.
3. Use [Jackson](https://github.com/FasterXML/jackson) to marshall to Map and back to new instance of POJO.
  * Check copy.equals(original)
  * Check copy.hashCode() == original.hashCode()
4. Use [Jackson](https://github.com/FasterXML/jackson) to marshall to json string and back to new instance of POJO.
  * Check copy.equals(original)
  * Check copy.hashCode() == original.hashCode()
5. Create variants by executing each setter with value. If the Bean has a [Lombok @Builder](https://projectlombok.org/features/Builder.html), the builder will be used to populate the bean instead of setters.
  * Check !variant.equals(original)
  * Execute steps 2-4 above
