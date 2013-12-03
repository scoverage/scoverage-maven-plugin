maven-scoverage-plugin
===================


[![Build Status](https://travis-ci.org/scoverage/maven-scoverage-plugin.png)](https://travis-ci.org/scoverage/maven-scoverage-plugin)

```xml
<!-- seperate compiler phases -->
<plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>
    <version>${maven.plugin.scala.version}</version>
    <configuration>
        <args>
            <arg>-g:vars</arg>
            <arg>-Yrangepos</arg>
        </args>
        <jvmArgs>
            <jvmArg>-Xms64m</jvmArg>
            <jvmArg>-Xmx1024m</jvmArg>
        </jvmArgs>
    </configuration>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>add-source</goal>
                <goal>compile</goal>
            </goals>
            <configuration>
                <compilerPlugins>
                    <compilerPlugin>
                        <groupId>com.sksamuel.scoverage</groupId>
                        <artifactId>scalac-scoverage-plugin</artifactId>
                        <version>0.92.0</version>
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
<!-- disable surefire -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${maven.plugin.surefire.version}</version>
    <configuration>
        <skipTests>true</skipTests>
    </configuration>
</plugin>
<!-- enable scalatest -->
<plugin>
    <groupId>org.scalatest</groupId>
    <artifactId>scalatest-maven-plugin</artifactId>
    <version>1.0-RC2</version>
    <executions>
        <execution>
            <id>test</id>
            <phase>test</phase>
            <goals>
                <goal>test</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <argLine>-XX:MaxPermSize=512m -Xmx1024m</argLine>
    </configuration>
</plugin>       
...
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-all</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>${scala.version}</version>
        </dependency>
        <dependency>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest_2.10</artifactId>
            <version>2.0</version>
        </dependency>
        <dependency>
            <groupId>com.sksamuel.scoverage</groupId>
            <artifactId>scalac-scoverage-plugin</artifactId>
            <version>0.92.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
```


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
