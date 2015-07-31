#!/bin/bash

git stash
find . -name pom.xml | xargs sed -i '' 's/${apollo-version}/1.7.1_2.11-SNAPSHOT/g'
mvn deploy -Dmaven.test.skip=true

git reset --hard
git stash pop
