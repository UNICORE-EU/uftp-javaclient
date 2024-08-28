# UFTP client

This repository contains the source code for the
'uftp' commandline client application.

## Download and installation

Working binaries can be downloaded via
[GitHub releases](https://github.com/UNICORE-EU/uftp-javaclient/releases)

To run, you'll need a Java runtime (version 11 or later).

## Usage

See the [manual](https://uftp-docs.readthedocs.io/en/latest/user-docs/uftp-client/index.html)

## Building from source

To build from source you need Java (11+) and Apache Maven.
Check the versions given in the pom.xml file.

The following commands create distribution packages
in zip, tgz, deb and rpm formats.

 * zip
```
mvn install -DskipTests
```

 * tar.gz

```
mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz
```

 * Debian

```
mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian
```

 * RedHat

```
mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat
```
