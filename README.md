scoverage-maven-plugin
===================

scoverage-maven-plugin is a plugin for Maven that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://travis-ci.org/scoverage/scoverage-maven-plugin.png)](https://travis-ci.org/scoverage/scoverage-maven-plugin)


## How to use

In short:

- **check** goal compiles classes with instrumentation, runs tests and checks coverage,

- **report** goal compiles classes with instrumentation, runs tests and generates html report as part of project's site,

- **report-only** goal generates html report as part of project's site using coverage data generated earlier in the build (in most cases by **check** goal),

- **pre-compile** and **post-compile** are internal goals, don't use them,

- **check-only** goal only check coverage, honestly I don't know if it will be usable at all. 

Check [all plugin goals (mojos) documentation](http://scoverage.github.io/scoverage-maven-plugin/1.0.4/plugin-info.html).


##### Prerequisities / limitations

Plugin is compatible with two Maven Scala compiler plugins:

- [SBT Compiler Maven Plugin](https://code.google.com/p/sbt-compiler-maven-plugin/) - version **1.0.0-beta5** or later required,

- [Scala Maven Plugin](http://davidb.github.io/scala-maven-plugin/) - version **3.0.0** or later required, [addScalacArgs](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#addScalacArgs) and [analysisCacheFile](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#analysisCacheFile) configuration parameters cannot be set directly, use project properties 'addScalacArgs' and 'analysisCacheFile' instead.


##### Scoverage Maven plugin version

This can be set as project property.

```xml
<project>
    <properties>
        <scoverage.plugin.version>1.0.4</scoverage.plugin.version>
    </properties>
</project>
```


##### Scala version configuration

Plugin supports Scala 2.10.x and 2.11.x versions by automatically loading and configuring scalac-scoverage-plugin_2.10 or scalac-scoverage-plugin_2.11 Scalac SCoverage Plugin artifact. For this to work Scala version has to be set. It can be done by defining "scalaVersion" plugin configuration parameter or "scala.version" project property. Without this setting, coverage will not be calculated. 

```xml
<project>
    <properties>
        <scala.version>2.11.4</scala.version>
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
                    <scalaVersion>2.11.4</scalaVersion>
                    <-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

The first method is better because once the property is defined it's value can be used in other places of the build file. For example in scala-library dependency version every Scala build should declare. 

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
It can be configured by defining "scalacPluginVersion" plugin configuration parameter or "scoverage.scalacPluginVersion" project property.

```xml
<project>
    <properties>
        <scoverage.scalacPluginVersion>1.0.4</scoverage.scalacPluginVersion>
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
                    <scalacPluginVersion>1.0.4</scalacPluginVersion>
                    <-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

##### Adding SCoverage report to site

Just add it to reporting section of your project. 

```xml
<project>
    <reporting>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
            </plugin>
        </plugins>
    </reporting>
</project>
```

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

There are no configuration parameters for report generating part of the plugin, so no configuration should be added inside 'reporting' section. 


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
                            <goal>check</goal>
                        </goals>
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
                            <goal>check</goal>
                        </goals>
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

There are many [example projects](https://github.com/scoverage/scoverage-maven-samples/tree/scoverage-maven-samples-1.0.4/).
Go to one of them and run `mvn site`.

## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014 Grzegorz Slowikowski

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
