#!/usr/bin/env sh

#
# Copyright 2020 AppsFlyer
#
# Licensed under the Apache License, Version 2.0 (the "License")
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

set -ev
mvn clean verify -Pcoverage jacoco:report coveralls:report -DdryRun=true -B -V
# shellcheck disable=SC1010
lein do kibit, coveralls, run -m coveralls-report
curl -F 'json_file=@target/coveralls/coveralls.json' 'https://coveralls.io/api/v1/jobs'