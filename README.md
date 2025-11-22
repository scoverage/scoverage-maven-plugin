scoverage-maven-plugin
===================

scoverage-maven-plugin is a plugin for Maven that integrates the scoverage code coverage library. Find out more about [scoverage](https://github.com/scoverage/scalac-scoverage-plugin).

[![Build Status](https://github.com/scoverage/scoverage-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/scoverage/scoverage-maven-plugin/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.sml.io/sonatype-central/org.scoverage/scoverage-maven-plugin/badge.svg)](https://maven-badges.sml.io/sonatype-central/org.scoverage/scoverage-maven-plugin/)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Gitter](https://img.shields.io/gitter/room/scoverage/scoverage.svg)](https://gitter.im/scoverage/scoverage-maven-plugin)

### How to use

mostly used mojos:

- **[check](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/check-mojo.html)** goal compiles classes with instrumentation, runs unit tests and checks coverage,

- **[report](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/report-mojo.html)** goal compiles classes with instrumentation, runs unit tests and generates reports,

- **[integration-check](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/integration-check-mojo.html)** goal compiles classes with instrumentation, runs unit and integration tests and checks coverage,

- **[integration-report](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/integration-report-mojo.html)** goal compiles classes with instrumentation, runs unit and integration tests and generates reports,

additional, sometimes useful, mojos:

- **[test](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/test-mojo.html)** goal compiles classes with instrumentation and runs unit tests,

- **[integration-test](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/integration-test-mojo.html)** goal compiles classes with instrumentation and runs unit and integration tests,

- **[check-only](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/check-only-mojo.html)** goal only checks coverage using coverage data generated earlier in the build (by **test**, **report**, **integration-test** or **integration-report** goal).

- **[report-only](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/report-only-mojo.html)** goal generates reports using coverage data generated earlier in the build (by **test**, **check**, **integration-test** or **integration-check** goal),

- **[package](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/package-mojo.html)** goal generates artifact file containing instrumented classes (e.g. for testing outside of the Maven build),

internal mojos:

- **[pre-compile](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/pre-compile-mojo.html)** and **[post-compile](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/post-compile-mojo.html)** are internal goals, they configure Maven build in forked `scoverage` life cycle; don't use them.

### Maven generated plugin documentation

| Version  | Documentation                                                                            |
|----------|------------------------------------------------------------------------------------------|
| `2.1.2`  | [Plugin Info](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/plugin-info.html)  |
| `1.4.11` | [Plugin Info](http://scoverage.github.io/scoverage-maven-plugin/1.4.11/plugin-info.html) |

### Prerequisites / limitations

- The plugin is compatible with [Scala Maven Plugin](http://davidb.github.io/scala-maven-plugin/) 
  - version `3.0.0` or later required
  - [addScalacArgs](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#addScalacArgs) and [analysisCacheFile](http://davidb.github.io/scala-maven-plugin/compile-mojo.html#analysisCacheFile) configuration parameters cannot be set directly, use project properties `addScalacArgs` and `analysisCacheFile` instead.
- Starting with version `2.1.0` JDK `17+`, Maven `3.6.3` and `maven-site-plugin` `3.20.0+` are required.
- Starting with version `2.0.0` the plugin supports Scala `2.12.8+`, `2.13.0+` and `3.2.0+`. For Scala `2.12.7` and lower please use version `1.4.11` of the plugin.
- The plugin is not thread-safe, so it should not be used in multi-threaded builds.


### Scoverage Maven plugin version

This can be set as project property.

```xml
<project>
    <properties>
        <scoverage.plugin.version>2.1.2</scoverage.plugin.version>
    </properties>
</project>
```


### Scala version configuration

The plugin automatically loads and configures matching `scalac-scoverage-plugin` artifact when needed. 
For this to work the Scala version has to be set. It can be done by defining the `scalaVersion` plugin configuration parameter or the `scala.version` project property. 
Without this setting, the plugin will try to resolve the Scala version from the `scala-library` dependency of the project. 
If all the attempts to resolve the Scala version fail, then coverage will not be calculated. 

```xml
<project>
    <properties>
        <scala.version>2.13.17</scala.version>
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
                    <scalaVersion>2.13.17</scalaVersion>
                    <!-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

The first method is better because once the property is defined its value can be used in other places of the build file. 
For example in the `scala-library` dependency version every Scala build should declare. 

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

### Scalac SCoverage plugin version configuration

Maven SCoverage plugin uses by default the latest version of the [scalac-scoverage-plugin](https://github.com/scoverage/scalac-scoverage-plugin) available on its release day.
If newer, better version of [scalac-scoverage-plugin](https://github.com/scoverage/scalac-scoverage-plugin) is available, it can be used instead.
It can be configured by defining the `scalacPluginVersion` plugin configuration parameter or the `scoverage.scalacPluginVersion` project property.

```xml
<project>
    <properties>
        <scoverage.scalacPluginVersion>2.4.2</scoverage.scalacPluginVersion>
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
                    <scalacPluginVersion>2.4.2</scalacPluginVersion>
                    <!-- other parameters -->
                </configuration>
             </plugin>
        </plugins>
    </build>
</project>
```

### Integration tests coverage check and reports

`integration-check` and `integration-report` mojos are similar to `check` and `report` mojos, but they execute forked `scoverage` life cycle up to `verify` phase (integration tests are usually executed in `integration-test` phase).

### Aggregated reports for multi-module projects

There is no separate mojo for aggregated reports, there is `aggregate` parameter.
To additionally generate aggregated SCoverage report for root module, when generating regular reports,
set `aggregate` parameter value to `true`.
It works only in multimodule projects, the aggregated report will be generated in the current
execution root.

It can be configured by defining the `aggregate` plugin configuration parameter or the `scoverage.aggregate` project property.

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

Since version `1.4.0-M5` it's possible to generate aggregated report only, without generating reports for individual modules. For large projects it can decrease build time significantly.

To generate only aggregated SCoverage report, set `aggregateOnly` parameter value to `true`. It works only in multimodule projects.

It can be configured by defining the `aggregateOnly` plugin configuration parameter or the `scoverage.aggregateOnly` project property.

```xml
<project>
    <properties>
        <scoverage.aggregateOnly>true</scoverage.aggregateOnly>
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
                    <aggregateOnly>true</aggregateOnly>
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
                    <aggregateOnly>true</aggregateOnly>
                </configuration>
             </plugin>
        </plugins>
    </reporting>
</project>
```

### Adding SCoverage report to site

Add the plugin to reporting section of your project and configure it to generate one of reporting mojos.
By default, Maven executes all plugin's reporting mojos, but the SCoverage plugin has three such mojos,
and it does not make sense to execute them all because every executed report will overwrite the previous one.
Configure one of them depending on your case.

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

| Reporting mojo                                                                                             | When                                                                                                                                                                                                                                                    |
|------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [report](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/report-mojo.html)                         | When not using integration tests (most cases)                                                                                                                                                                                                           |
| [integration-report](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/integration-report-mojo.html) | When using integration tests                                                                                                                                                                                                                            |
| [report-only](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/report-only-mojo.html)               | When coverage data was already generated (usually by [check](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/check-mojo.html) or [integration-check](http://scoverage.github.io/scoverage-maven-plugin/2.1.2/integration-check-mojo.html) mojo) |

### Customizing code instrumentation

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

Read [SBT SCoverage Plugin documentation](https://github.com/scoverage/sbt-scoverage) for more information about
[excludedPackages and excludedFiles](https://github.com/scoverage/sbt-scoverage?tab=readme-ov-file#exclude-classes-and-packages-and-files).

File and package exclusions are supported for Scala 2, Scala [`3.3.4`-`3.4.0`) and `3.4.2`+.

### Checking minimum test coverage level

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <minimumCoverage>95</minimumCoverage>
                    <minimumCoverageBranchTotal>90</minimumCoverageBranchTotal>
                    <minimumCoverageStmtPerPackage>90</minimumCoverageStmtPerPackage>
                    <minimumCoverageBranchPerPackage>85</minimumCoverageBranchPerPackage>
                    <minimumCoverageStmtPerFile>85</minimumCoverageStmtPerFile>
                    <minimumCoverageBranchPerFile>80</minimumCoverageBranchPerFile>
                    <failOnMinimumCoverage>true</failOnMinimumCoverage>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal> <!-- or integration-check -->
                        </goals>
                    </execution>
                </executions>
             </plugin>
        </plugins>
    </build>
</project>
```

Run `mvn scoverage:check` to perform the check. See below if you want to use `mvn verify` to perform the check.

Read [SBT SCoverage Plugin documentation](https://github.com/scoverage/sbt-scoverage#minimum-coverage) for more information. 


### Checking minimum test coverage level AND adding report to site

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

Run `mvn scoverage:check` to perform the check and `mvn scoverage:report` to generate the report.


### Checking minimum test coverage level automatically

If you want `mvn verify` and `mvn install` to check the coverage level, you have to change your POM
so that SCoverage takes over running all the tests.

The reason for this is that SCoverage instruments classes during compilation and writes them to disk.
We don't want to accidentally deploy these instrumented classes, so SCoverage keeps them separate.
SCoverage does this by forking the current Maven build and running it again, while performing instrumentation.
In a normal setup this causes the tests to be run twice: once in the outer run with the original classes
and once in the SCoverage-forked run with the instrumented classes.

Since version `1.4.0-M5` it's possible to make the tests run only once. You have to configure your pom to turn off testing in the outer run and tell SCoverage to run all tests in the forked run.

This example shows the required configuration:

```xml
<project>
    <properties>
        <skipTests>true</skipTests> <!-- disable surefire and failsafe tests -->
    </properties>
...
    <build>
        <plugins>
            <plugin>
                <groupId>org.scoverage</groupId>
                <artifactId>scoverage-maven-plugin</artifactId>
                <version>${scoverage.plugin.version}</version>
                <configuration>
                    <minimumCoverage>80</minimumCoverage>
                    <failOnMinimumCoverage>true</failOnMinimumCoverage>

                    <!-- enable surefire and failsafe tests in SCoverage -->
                    <additionalForkedProjectProperties>skipTests=false</additionalForkedProjectProperties>
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

Run `mvn clean verify` or `mvn clean install` to run the tests with coverage and all other static analysis you have configured.

If you want to set multiple properties from within `<additionalForkedProjectProperties>`, for instance because you want to disable other plugins from running twice, you can separate them with a semicolon:

`<additionalForkedProjectProperties>skipTests=false;skip.scalafmt=false</additionalForkedProjectProperties>`


## Examples
There are many example projects in [integration tests](src/it) directory. To run them, execute `mvn integration-test`. 
To execute only one of them, execute `mvn integration-test -Dinvoker.test=test_aggregate`, where `test_aggregate` is the name of the directory with the example project.

Also, there are many [example projects](https://github.com/scoverage/scoverage-maven-samples/tree/scoverage-maven-samples-1.4.11/) for older versions of the plugin in a separate repo.
Go to one of them and run `mvn site`.

## Development

### Snapshot deployment
A new snapshot is deployed to sonatype on each commit merged to `main` branch. See [snapshot.yml](.github/workflows/snapshot.yml) workflow for details.

### New release deployment

A new release can be done on demand using `Release new version` workflow. See [release.yml](.github/workflows/release.yml) for details.

#### Steps
1. [Run](https://github.com/scoverage/scoverage-maven-plugin/actions/workflows/release.yml) `Release new version` workflow.
2. Wait ~ 8 hours for sync to maven central.
3. Check jars are available in maven central https://search.maven.org/artifact/org.scoverage/scoverage-maven-plugin and GH label in README is updated.
4. Checkout new tag, e.g. `git checkout scoverage-maven-plugin-2.1.2`.
5. Generate site docs with `mvn clean site -P publicsite` and check that all required docs are generated in `./target/site`.
6. Checkout `gh-pages` branch, e.g. `git checkout gh-pages`.
7. Copy generated site to new directory named as version number, e.g. `cp -r ./target/site ./2.1.2`, commit and push.
8. Wait several seconds for the docs to be published and verify, e.g. https://scoverage.github.io/scoverage-maven-plugin/2.1.2/check-mojo.html.
9. Checkout `main` branch, e.g. `git checkout main`.
10. Update versions in README, e.g. `sed -i 's/2.1.0/2.1.2/g' README.md`, commit and push.
11. Create new Release on GitHub.


This process will be fully or partially automated in the future.


#### Releasing locally

###### Prerequisites
Set sonatype credentials in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>sonatype-username-goes-here</username>
      <password>sonatype-token-goes-here</password>
    </server>
  </servers>
</settings>
```
Make sure you have proper pgp keys set up. See [Maven Central Guide](https://central.sonatype.org/pages/working-with-pgp-signatures.html) for details.

###### Steps

1. Run
`mvn -ntp -B release:clean release:prepare release:perform -P release -Darguments="-DskipTests"`.
2. Follow steps 2-11 above
   
## License
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014-2025 Grzegorz Slowikowski

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
