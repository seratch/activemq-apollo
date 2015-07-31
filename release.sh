#!/bin/bash

git stash
find . -name pom.xml | xargs sed -i '' 's/${apollo-version}/2.0.0-SNAPSHOT/g'
mvn deploy -Dmaven.test.skip=true

git reset --hard
git stash pop
