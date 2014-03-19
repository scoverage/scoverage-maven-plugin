maven-scoverage-plugin
===================

maven-scoverage-plugin is a plugin for Maven that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://travis-ci.org/scoverage/maven-scoverage-plugin.png)](https://travis-ci.org/scoverage/maven-scoverage-plugin)

## How to use

The maven support works in two ways. Firstly, you add a compiler plugin to the scala build which causes the source to be instrumented during the test run. Then secondly you run a maven plugin which converts the output of the instrumentation into the XML / HTML reports.

You must split the scala compiler into two phases - one for main sources and one for test sources - and attach the compiler plugin to the main sources phase. Otherwise your tests would also be included in the coverage metrics. Note the important compiler arguments.

> Note: The ``coverage.data.dir`` property is used to figure out which directory to read the coverage files from. Please make sure that the ``-P:scoverage:dataDir`` compiler arg is set to the same value as the ``coverage.data.dir`` property. Otherwise, the plugin will not be able to generate the reports.


```xml
<properties>
	<scala.major>2.10</scala.major>
	<coverage.data.dir>${project.build.outputDirectory}</coverage.datadir>
</properties>
...
<plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>
    <version>${maven.plugin.scala.version}</version>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>add-source</goal>
                <goal>compile</goal>
            </goals>
            <configuration>
		        <args>
            		<arg>-g:vars</arg>
            		<arg>-Yrangepos</arg>
            		<arg>-P:scoverage:dataDir:${coverage.data.dir}</arg>
        		</args>
        		<jvmArgs>
            		<jvmArg>-Xms64m</jvmArg>
            		<jvmArg>-Xmx1024m</jvmArg>
		        </jvmArgs>
                <compilerPlugins>
                    <compilerPlugin>
                        <groupId>com.sksamuel.scoverage</groupId>
                        <artifactId>scalac-scoverage-plugin_${scala.major}</artifactId>
                        <version>0.95.0</version>
                    </compilerPlugin>
                </compilerPlugins>
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
    <groupId>com.sksamuel.scoverage</groupId>
    <artifactId>scalac-scoverage-plugin_${scala.major}</artifactId>
    <version>0.95.0</version>
</dependency>
```

Finally, add the plugin to the build.

```xml
<build>
  <plugins>
    <plugin>
        <groupId>com.sksamuel.scoverage</groupId>
        <artifactId>maven-scoverage-plugin</artifactId>
        <version>0.95.0</version>
    </plugin>
  </plugins>
</build>

```

Then you can run your build as normal eg mvn clean test, or maven clean install.
After that you can run mvn scoverage:report to generate the XML / HTML reports which you will find inside ``${project.build.outputDirectory}/coverage-report. ``

Of course you can setup the plugin to run as part of the normal build, without having to enter mvn scoverage:report, by simply binding the plugin to a phase.

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Stephen Samuel

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
