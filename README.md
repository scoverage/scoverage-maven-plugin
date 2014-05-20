maven-scoverage-plugin
===================

maven-scoverage-plugin is a plugin for Maven that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://travis-ci.org/scoverage/maven-scoverage-plugin.png)](https://travis-ci.org/scoverage/maven-scoverage-plugin)

## How to use

The maven support works in two ways. Firstly, you add a compiler plugin to the scala build which causes the source to be instrumented during the test run. Then secondly you run a maven plugin which converts the output of the instrumentation into the XML / HTML reports.

You must split the scala compiler into two phases - one for main sources and one for test sources - 
and attach the compiler plugin to the main sources phase. 
Otherwise your tests would also be included in the coverage metrics. Also note the important compiler arguments.

There are two version numbers to be aware of. The maven plugin and the compiler plugin. These use similar version numbers but are not neccessarily the same - for instance the compiler plugin might be updated without needing a new release of the maven plugin. 

```xml
<properties>
	<scoverage-plugin.version>put version here of the compiler plugin eg 0.99.4</scoverage-plugin.version>
	<maven.plugin.scoverage.version>put version here of the maven plugin, eg 0.99.2</maven.plugin.scoverage.version>
	<scala.short>2.11</scala.short>
</properties>
...
<plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>
    <version>${scoverage-plugin.version}</version>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>add-source</goal>
                <goal>compile</goal>
            </goals>
            <configuration>
        		<jvmArgs>
            		<jvmArg>-Xms64m</jvmArg>
            		<jvmArg>-Xmx1024m</jvmArg>
		        </jvmArgs>
                <compilerPlugins>
                    <compilerPlugin>
                        <groupId>org.scoverage</groupId>
                        <artifactId>scalac-scoverage-plugin_${scala.short}</artifactId>
                        <version>${scoverage-plugin.version}</version>
                    </compilerPlugin>
                </compilerPlugins>
                <args>
                    <arg>-g:vars</arg>
                    <arg>-Yrangepos</arg>
                    <arg>-P:scoverage:dataDir:${project.build.outputDirectory}</arg>
                </args>
            </configuration>
        </execution>
        <execution>
            <id>test</id>
            <goals>
                <goal>add-source</goal>
                <goal>testCompile</goal>
            </goals>
        </execution>
    </executions>
</plugin>       
```

Include the dependencies on the compiler plugin. Versions must match the above.

```xml
<dependency>
    <groupId>org.scoverage</groupId>
    <artifactId>scalac-scoverage-plugin_${scala.short}</artifactId>
    <version>${scoverage-plugin.version}</version>
</dependency>
```

Finally, add the plugin to the build.

```xml
<build>
  <plugins>
    <plugin>
        <groupId>org.scoverage</groupId>
        <artifactId>maven-scoverage-plugin</artifactId>
        <version>${maven.plugin.scoverage.version}</version>
    </plugin>
  </plugins>
</build>
```

Then you can run your build as normal eg mvn clean test, or maven clean install.
After that you can run mvn scoverage:report to generate the XML / HTML reports which you will find inside ``${project.build.outputDirectory}/coverage-report. ``

Of course you can setup the plugin to run as part of the normal build, without having to invoke mvn scoverage:report, by simply binding the plugin to a phase:


```xml
<build>
  <plugins>
    <plugin>
        <groupId>org.scoverage</groupId>
        <artifactId>maven-scoverage-plugin</artifactId>
        <version>${scoverage-plugin.version}</version>
        <executions>
          <execution>
            <id>install</id>
            <goals>
                <goal>report</goal>
            </goals>
          </execution>
        </executions>
    </plugin>
  </plugins>
</build>
```

You can see a working maven example in the [samples project](https://github.com/scoverage/scoverage-samples). Clone 
that project and run `mvn clean test`.

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014 Stephen Samuel

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
