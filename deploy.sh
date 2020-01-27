
export GIT_LAST_TAG=`git describe --abbrev=0 --tags`
export GIT_COMMITS_SINCE_LAST_TAG=`git rev-list ${GIT_LAST_TAG}..HEAD --count`
export GIT_COMMIT_ID=`git rev-parse --short HEAD`
export GREYCAT_DEPS_VERSION="${GIT_LAST_TAG}.${GIT_COMMITS_SINCE_LAST_TAG}"
if [ $GIT_COMMITS_SINCE_LAST_TAG != "0" ]; then export GREYCAT_DEPS_VERSION="${GREYCAT_DEPS_VERSION}+${GIT_COMMIT_ID}"; fi
echo "VERSION=${GREYCAT_DEPS_VERSION}"
