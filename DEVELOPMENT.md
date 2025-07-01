# Development

Information for developers.

Jenkins version is set in `pom.xml` as `<jenkins.baseline>` property.

## Requirements

* Java 17
* Apache Maven 3.9.9

## Environment setup

* Java and Maven can be installed like it described into the guide https://www.jenkins.io/doc/developer/tutorial/prepare/
* Configure maven according to the guide https://www.jenkins.io/doc/developer/tutorial/prepare/#configure-apache-maven
* To start the local Jenkins instance run:<br>
<code>mvn hpi:run -Dport=8080</code>

## Installation instructions

* To build the project run
<code>mvn clean package</code>.<br>
It will create the plugin <code>.hpi</code> file.

* Copy the resulting <code><em>./target/jenkins_plugin.hpi</em></code> file into the <code>$JENKINS_HOME/plugins</code> directory. Don't forget to restart Jenkins afterward.

  * or use the plugin management console (http://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the plugin in the installed plugins list.

More detailed instructions are [there](https://www.jenkins.io/doc/book/managing/plugins/#advanced-installation
).

## Publishing release

The following links provide base information:
* https://www.jenkins.io/doc/developer/publishing/preparation/#signup-required
* https://www.jenkins.io/doc/developer/publishing/requesting-hosting/

*Note: most of the project data is ready to release but there are two parameters that can be rejected by Jenkins audit system:
* The minimum required version which can be set in `pom.xml` as `<jenkins.baseline>` property. Take a look at the [baseline recommendations](https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/#currently-recommended-versions) to get actual value.
* The BOM version which is related to `<jenkins.baseline>` and can be found in `pom.xml` as `<jenkinsBom.version>`. The list of BOM is available [there](https://repo.jenkins-ci.org/public/io/jenkins/tools/bom/) (should be selected the latest version however it should be highlighted by Jenkins audit system). 
