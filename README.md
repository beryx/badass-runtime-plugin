[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/beryx/badass-runtime-plugin/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/beryx/badass-runtime-plugin/master.svg?label=Build)](https://travis-ci.org/beryx/badass-runtime-plugin)

## Badass Runtime Plugin ##

A Gradle plugin to create custom runtime images for non-modularized applications. 

Badass-Runtime exposes an extension with the name `runtime` to let you configure various aspects of its operation.
A simple example configuration is shown below:

```
runtime {
    options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
    modules = ['java.naming', 'java.xml']
}
``` 
