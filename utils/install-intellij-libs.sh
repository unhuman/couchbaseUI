#!/bin/sh

# https://github.com/jorichard/ideauidesigner-maven-plugin/blob/master/install-intellij-libs.sh

INTELLIJ_HOME=$1
INTELLIJ_VERSION=$2

if [ -z "$INTELLIJ_HOME" ]
then
  echo "Please provide the path to Intellij home directory. For example: install-intellij-libs.sh /Applications/Applications/IntelliJ\ IDEA\ CE.app/"
  exit 1
fi

if [ -z "$INTELLIJ_VERSION" ]
then
  echo "Please provide the version of IntelliJ installed.  For exmaple: 2019.3.4"
  exit 1
fi

if [ ! -d "$INTELLIJ_HOME" ]
then
  echo "Directory does not exist: $INTELLIJ_HOME"
  exit 1
fi

echo 'Installing Intellij artifacts to Maven local repository'
echo "Intellij home: $INTELLIJ_HOME"

mvn install:install-file -Dfile="$INTELLIJ_HOME/lib/javac2.jar" -DgroupId=com.intellij -DartifactId=javac2 -Dversion="$INTELLIJ_VERSION" -Dpackaging=jar
mvn install:install-file -Dfile="$INTELLIJ_HOME/lib/asm-all-7.0.1.jar" -DgroupId=com.intellij -DartifactId=asm-all -Dversion="$INTELLIJ_VERSION" -Dpackaging=jar
mvn install:install-file -Dfile="$INTELLIJ_HOME/lib/forms_rt.jar" -DgroupId=com.intellij -DartifactId=forms_rt -Dversion="$INTELLIJ_VERSION" -Dpackaging=jar
