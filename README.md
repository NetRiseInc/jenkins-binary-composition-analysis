# NetRise Plugin

## Introduction

A Jenkins plugin to upload build artifacts to the NetRise platform for analysis.

## Getting started

Information for developers can be found [there](./DEVELOPMENT.md).

Instructions to install this plugin can be found [there](https://www.jenkins.io/doc/book/managing/plugins/#installing-a-plugin).

### Configuration options

To configure this plugin, go to: <em>Manage Jenkins -> Configure System -> NetRise</em>.

All the parameters are mandatory to input:
* Organization ID
* Endpoint
* Client ID
* Client Secret
* Token URL
* Audience

### Usage examples

This plugin can be used in both freestyle or pipeline projects. The freestyle project configuration quite straightforward so there is no need to make an example. All you need is just to select <b>NetRise plugin</b> as build step and input parameters:

* Artifact
* Name
* Model (optional)
* Version (optional)
* Manufacturer (optional)

Here is the pipeline example:

<pre>
pipeline {
    agent any
    stages { // this stage is needed only to create artifact
        stage('Create File') {
            steps {
                writeFile file: 'artefact.sh', text: 'echo "Hello Jenkins!"'
            }
        }
        stage('Run upload to NetRise') {
            steps {
                uploadToNetRise(
                    artifact: 'artefact.sh', 
                    name: 'Asset name ${BUILD_ID}', 
                    model: 'Model', 
                    version: 'Version 1', 
                    manufacturer: 'Manufacturer'
                )
            }
        }
    }
}
</pre>

All the parameters support environmental variables (see <code>name</code> in pipeline example).

## Troubleshooting guide

## Contact information

https://netrise.io

## License

Licensed under MIT, see [LICENSE](LICENSE.md)