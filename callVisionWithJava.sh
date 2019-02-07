#!/bin/bash 
# Copyright 2018 Google LLC. This software is provided as-is, without warranty or representation for any use or purpose. Your use of it is subject to your agreements with Google.  
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# “Copyright 2018 Google LLC. This software is provided as-is, without warranty or representation for any use or purpose.
#  Your use of it is subject to your agreements with Google.”
#

# Author: Sufyaan Kazi
# Date: April 2018
# Purpose: Use Google Cloud Natural Language API to extract information about people, places, events etc from text
#   and Speech API to analyse some speech
# https://google.qwiklabs.com/focuses/109?parent=catalog
# https://google.qwiklabs.com/focuses/115?parent=catalog

. ./common.sh

main() {
  local APIS="iam cloudresourcemanager language speech vision"
  local PROJECT_ID=$(gcloud config list project --format "value(core.project)")
  local SCRIPT_NAME=gcloud-ml
  local SERVICE_ACC=svcacc-$SCRIPT_NAME@$PROJECT_ID
  local KEY_FILE=../keys/$SERVICE_ACC.json
  local ROLES=roles/viewer
  local BUCKET=imagestoocr
  local IMAGE_PATH=Modified.png
  declare -a ANNOTATIONS=(detect-document detect-faces detect-image-properties detect-labels detect-landmarks detect-logos detect-safe-search detect-text detect-web suggest-crop)
  local DATE=$(date '+%Y%m%d%H:%M:%S')
  local OUTPUT_FOLDER=./output/

  #Enable required GCP apis
  enableAPIs $APIS
  printf "******\n\n"

  # Get the current active authorised account, because we are also using
  # curl, we need to switch authorised accounts, then switch back after
  echo "Determining current GCP account"
  export ACTIVE_ACC=$(gcloud auth list | grep '*' | xargs | cut -d " " -f2)
  echo "You are currently logged into GCP as: $ACTIVE_ACC"
  printf "******\n\n"

  echo "Checking to see if a Service account needs to be created"
  createServiceAccount $SCRIPT_NAME $PROJECT_ID $KEY_FILE $ROLES
  printf "******\n\n"

  echo "Detect Text in an image using vision API"
  echo "Reading image: gs://$BUCKET/$IMAGE_PATH"
  #for annotation in "${ANNOTATIONS[@]}"
  #do
    #echo "Calling vision $annotation, writing to $OUTPUT_FOLDER/vision_${annotation}_${DATE}.json"
    #gcloud ml vision $annotation "gs://$BUCKET/$IMAGE_PATH" > $OUTPUT_FOLDER/vision_${annotation}_${DATE}.json
  #done
  ./gradlew build run --args="gs://$BUCKET/$IMAGE_PATH"
  printf "******\n\n"

}

set -e
trap 'abort' 0
SECONDS=0
main
trap : 0
gcloud config set account $ACTIVE_ACC
echo "Script completed in ${SECONDS} seconds."
