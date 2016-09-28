scoverage-maven-plugin
===================

scoverage-maven-plugin is a plugin for Maven that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Join the chat at https://gitter.im/scoverage/scoverage-maven-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scoverage/scoverage-maven-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/scoverage/scoverage-maven-plugin.png?branch=master)](https://travis-ci.org/scoverage/scoverage-maven-plugin)
[![Build Status](https://circleci.com/gh/scoverage/scoverage-maven-plugin/tree/master.svg?&style=shield)](https://circleci.com/gh/scoverage/scoverage-maven-plugin)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scoverage/scoverage-maven-plugin/badge.png?style=flat)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scoverage-maven-plugin%22)


## How to use

mostly used mojos:

- **[check](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/check-mojo.html)** goal compiles classes with instrumentation, runs unit tests and checks coverage,

- **[report](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/report-mojo.html)** goal compiles classes with instrumentation, runs unit tests and generates reports,

- **[integration-check](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/integration-check-mojo.html)** goal compiles classes with instrumentation, runs unit and integration tests and checks coverage,

- **[integration-report](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/integration-report-mojo.html)** goal compiles classes with instrumentation, runs unit and integration tests and generates reports,

additional, sometimes useful, mojos:

- **[test](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/test-mojo.html)** goal compiles classes with instrumentation and runs unit tests,

- **[integration-test](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/integration-test-mojo.html)** goal compiles classes with instrumentation and runs unit and integration tests,

- **[check-only](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/check-only-mojo.html)** goal only checks coverage using coverage data generated earlier in the build (by **test**, **report**, **integration-test** or **integration-report** goal).

- **[report-only](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/report-only-mojo.html)** goal generates reports using coverage data generated earlier in the build (by **test**, **check**, **integration-test** or **integration-check** goal),

- **[package](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/package-mojo.html)** goal generates artifact file containing instrumented classes (e.g. for testing outside of the Maven build),

internal mojos:

- **[pre-compile](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/pre-compile-mojo.html)** and **[post-compile](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/post-compile-mojo.html)** are internal goals, they configure Maven build in forked `scoverage` life cycle; don't use them.

Maven generated plugin documentation:

| Version          | Documentation                                                                                    |
|------------------|--------------------------------------------------------------------------------------------------|
| `1.2.0`          | [Plugin Info](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/plugin-info.html)          |
| `1.3.0-SNAPSHOT` | [Plugin Info](http://scoverage.github.io/scoverage-maven-plugin/1.3.0-SNAPSHOT/plugin-info.html) |

##### Prerequisities / limitations

Plugin is compatible with two Maven Scala compiler plugins:

- [SBT Compiler Maven Plugin](https://github.com/sbt-compiler-maven-plugin/sbt-compiler-maven-plugin/) - version **1.0.0-beta5** or later required,

- [Scala Maven Plugin](http://davidb.github.io/scala-maven-plugin/) - version **3.0.0** or later required, [addScalacArgs](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#addScalacArgs) and [analysisCacheFile](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#analysisCacheFile) configuration parameters cannot be set directly, use project properties 'addScalacArgs' and 'analysisCacheFile' instead.


##### Scoverage Maven plugin version

This can be set as project property.

```xml
<project>
    <properties>
        <scoverage.plugin.version>1.2.0</scoverage.plugin.version>
    </properties>
</project>
```


##### Scala version configuration

Plugin supports Scala 2.10.x and 2.11.x versions by automatically loading and configuring scalac-scoverage-plugin_2.10 or scalac-scoverage-plugin_2.11 Scalac SCoverage Plugin artifact. For this to work Scala version has to be set. It can be done by defining `scalaVersion` plugin configuration parameter or `scala.version` project property. Without this setting, coverage will not be calculated. 

```xml
<project>
    <properties>
        <scala.version>2.11.8</scala.version>
    </properties>
</project>
```

or

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <scalaVersion>2.11.8</scalaVersion>
                    <-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

The first method is better because once the property is defined it's value can be used in other places of the build file. For example in `scala-library` dependency version every Scala build should declare. 

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>${scala.version}</version>
        </dependency>
    </dependencies>
</project>
```

##### Scalac SCoverage plugin version configuration

Maven SCoverage plugin uses by default the latest version of the [scalac-scoverage-plugin](https://github.com/scoverage/scalac-scoverage-plugin) available on its release day.
If newer, better version of [scalac-scoverage-plugin](https://github.com/scoverage/scalac-scoverage-plugin) is available, it can be used instead.
It can be configured by defining `scalacPluginVersion` plugin configuration parameter or `scoverage.scalacPluginVersion` project property.

```xml
<project>
    <properties>
        <scoverage.scalacPluginVersion>1.2.0</scoverage.scalacPluginVersion>
    </properties>
</project>
```

or

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <scalacPluginVersion>1.2.0</scalacPluginVersion>
                    <-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

##### Integration tests coverage check and reports

`integration-check` and `integration-report` mojos are similar to `check` and `report` mojos, but they execute forked `scoverage` life cycle up to `verify` phase (integration tests are usually executed in `integration-test` phase).

##### Aggregated reports for multi-module projects

There is no separate mojo for aggregated reports, there is `aggregate` parameter.
To additionally genenerate aggregated SCoverage report for root module, when generating regular reports, set `aggregate` parameter value to `true`.
It works only in multimodule projects.

It can be configured by defining `aggregate` plugin configuration parameter or `scoverage.aggregate` project property.

```xml
<project>
    <properties>
        <scoverage.aggregate>true</scoverage.aggregate>
    </properties>
</project>
```

in `build/plugins` or `build/pluginManagement` section when running reports directly from console (e.g. `mvn scoverage:report`)

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <aggregate>true</aggregate>
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

or in `reporting/plugins` section when adding report to Maven generated site

```xml
<project>
    <reporting>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <aggregate>true</aggregate>
                </configuration>
             </plugin>
        </plugins>
    </reporting>
</project>
```

##### Adding SCoverage report to site

Add plugin to reporting section of your project and configure it to generate one of reporting mojos.
By default Maven executes all plugin's reporting mojos, but in case of SCoverage plugin this does not make sense, every executed report will overwrite the previous one.

```xml
<project>
    <reporting>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <reportSets>
                    <reportSet>
                        <reports>
                            <report>report</report>
                            <!-- or <report>integration-report</report> -->
                            <!-- or <report>report-only</report> -->
                        </reports>
                    </reportSet>
                </reportSets>
            </plugin>
        </plugins>
    </reporting>
</project>
```

Which reporting mojo should be selected:

| Reporting mojo       | When                                          |
|----------------------|-----------------------------------------------|
| [report](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/report-mojo.html) | When not using integration tests (most cases) |
| [integration-report](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/integration-report-mojo.html) | When using integration tests                  |
| [report-only](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/report-only-mojo.html) | When coverage data was already generated (usually by [check](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/check-mojo.html) or [integration-check](http://scoverage.github.io/scoverage-maven-plugin/1.2.0/integration-check-mojo.html) mojo) |

##### Customizing code instrumentation

If you want to customize plugin's configuration parameters used by compilation supporting part of the plugin, do it in 'plugins' or 'pluginManagement' section:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <highlighting>true</highlighting>
                    <!-- example configuration for Play! Framework 2.x project -->
                    <excludedPackages>views.html.*</excludedPackages> 
                    <excludedFiles>.*?routes_(routing|reverseRouting)</excludedFiles>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Read [SBT SCoverage Plugin documentation](https://github.com/scoverage/sbt-scoverage) for more information about [highlighting](https://github.com/scoverage/sbt-scoverage#highlighting) and [excludedPackages](https://github.com/scoverage/sbt-scoverage#exclude-classes-and-packages).


##### Checking minimum test coverage level

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <minimumCoverage>80</minimumCoverage>
                    <failOnMinimumCoverage>true</failOnMinimumCoverage>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal> <!-- or integration-check -->
                        </goals>
                        <phase>prepare-package</phase> <!-- or any other phase -->
                    </execution>
                </executions>
             </plugin>
        </plugins>
    </build>
</project>
```

Read [SBT SCoverage Plugin documentation](https://github.com/scoverage/sbt-scoverage#minimum-coverage) for more information. 


##### Checking minimum test coverage level AND adding report to site

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <minimumCoverage>80</minimumCoverage>
                    <failOnMinimumCoverage>true</failOnMinimumCoverage>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal> <!-- or integration-check -->
                        </goals>
                        <phase>prepare-package</phase> <!-- or any other phase -->
                    </execution>
                </executions>
             </plugin>
        </plugins>
    </build>

    <reporting>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <reportSets>
                    <reportSet>
                        <reports>
                            <report>report-only</report>
                        </reports>
                    </reportSet>
                </reportSets>
            </plugin>
        </plugins>
    </reporting>
</project>
```

There are many [example projects](https://github.com/scoverage/scoverage-maven-samples/tree/scoverage-maven-samples-1.2.0/).
Go to one of them and run `mvn site`.

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014-2016 Grzegorz Slowikowski

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
