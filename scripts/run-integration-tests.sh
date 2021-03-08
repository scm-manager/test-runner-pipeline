#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

rm -rf integration-test-runner
git clone https://github.com/scm-manager/integration-test-runner
cd integration-test-runner
yarn install
# Ugly workaround till we release the test-runner
yarn link
yarn link @scm-manager/integration-test-runner
yarn integration-test-runner collect -c -s
yarn integration-test-runner provision -a ${SERVER_URL} -u scmadmin -p scmadmin
yarn integration-test-runner run -a ${SERVER_URL} -u scmadmin -p scmadmin
