#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

rm -rf integration-test-runner
git clone https://github.com/scm-manager/integration-test-runner
cd integration-test-runner
yarn install
yarn integration-test-runner collect -c -s
yarn integration-test-runner provision -a ${SERVER_URL} -u scmadmin -p scmadmin
yarn integration-test-runner run -a ${SERVER_URL} -u scmadmin -p scmadmin
