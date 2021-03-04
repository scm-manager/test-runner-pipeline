#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

rm -rf integration-test-runner
git clone https://github.com/scm-manager/integration-test-runner
cd integration-test-runner
yarn install
ls -l
yarn integration-test-runner collect -c
yarn integration-test-runner provision -a http://scm-server:8080/scm -u scmadmin -p scmadmin
yarn integration-test-runner run -a http://scm-server:8080/scm -u scmadmin -p scmadmin
