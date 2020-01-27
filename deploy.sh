#
# Copyright 2017 The Java2TypeScript Authors.  All rights reserved.
# <p>
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# http://www.apache.org/licenses/LICENSE-2.0
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


export GIT_LAST_TAG=`git describe --abbrev=0 --tags`
export GIT_COMMITS_SINCE_LAST_TAG=`git rev-list ${GIT_LAST_TAG}..HEAD --count`
export GIT_COMMIT_ID=`git rev-parse --short HEAD`
export REPO_VERSION="${GIT_LAST_TAG}.${GIT_COMMITS_SINCE_LAST_TAG}"
echo "VERSION=${REPO_VERSION}"

mvn licence:format
mvn versions:set -DnewVersion="${REPO_VERSION}"
mvn clean install

mvn ${MAVEN_CLI_OPTS} ${MAVEN_OPTS} deploy:deploy-file -DpomFile=pom.xml -Dfile=../../dist/${GREYCAT_VERSION}/core-${GREYCAT_VERSION}.jar -Dsources=../../dist/${GREYCAT_VERSION}/core-${GREYCAT_VERSION}-sources.jar -DrepositoryId=greycat-core -Durl=https://dist.datathings.com/repository/greycat-core-maven
