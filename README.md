# BrowserStack Assignment by Ayush Agarwal

WebScrapping Spanish NewsArticle Website using Selenium Web Automation

### Test IDs of Successful Test Ran across 5 parallel threads on Desktop and Mobile Browsers

Build UUID - `gnoysm5i8nbf3pwubqwqexegkofxh4vipytel4pt`

1. Iphone 16 Pro Max IOS 18 (Safari) - `f2d453a2d3e16729c54416de73feb2f3963b6bd8`

2. Samsung Galaxy S24 ULTRA (Chrome) - `8d82a973d857bd0e8b3cdb818da2c0fc352a68eb`

3. Windows 11 (Firefox) - `0bbaa6964e07e593a54b1ce5de25076e80ad9e9d`

4. MAC OS X Sequoia(Chrome) - `89e23d840966e4bcee13a51b141a641522338acc`

5. Windows 11 (Edge) - `65695b3c6428667951162fb41bf145dbbbfe8b3c`
## Maven


- Clone the repository
- Replace "YOUR_USERNAME" and "YOUR_ACCESS_KEY" with your BrowserStack access credentials in "browserstack.yml".
- Install dependencies `mvn compile`
- Update dependencies `mvn clean install -U` for outdated dependencies.
- To run test, run `mvn clean test`
- To run the test suite having cross-platform with parallelization, run `mvn test -D suite=config/bs-bsTest.testng.xml`
- To run local test, run `mvn test -D suite=config/bs-localTest.testng.xml`


### Integrate your test suite

This repository uses the BrowserStack SDK to run tests on BrowserStack. Follow the steps below to install the SDK in your test suite and run tests on BrowserStack:

* Create sample browserstack.yml file with the browserstack related capabilities with your [BrowserStack Username and Access Key](https://www.browserstack.com/accounts/settings) and place it in your root folder.
* Add maven dependency of browserstack-java-sdk in your pom.xml file
```sh
<dependency>
    <groupId>com.browserstack</groupId>
    <artifactId>browserstack-java-sdk</artifactId>
    <version>LATEST</version>
    <scope>compile</scope>
</dependency>
```
* Modify your build plugin to run tests by adding argLine `-javaagent:${com.browserstack:browserstack-java-sdk:jar}` and `maven-dependency-plugin` for resolving dependencies in the profiles `Browserstack-test`.

            <plugin>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>getClasspathFilenames</id>
                        <goals>
                            <goal>properties</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>${config.file}</suiteXmlFile>
                    </suiteXmlFiles>
                    <argLine>
                        -javaagent:${com.browserstack:browserstack-java-sdk:jar}
                    </argLine>
                </configuration>
            </plugin>
```
* Install dependencies `mvn compile` 
```

#### Author- Ayush Agarwal
