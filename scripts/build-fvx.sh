#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "${SCRIPT_DIR}/.." && pwd)

UPR_FVX_VERSION=${UPR_FVX_VERSION:-1.5.1}
UPR_FVX_TAG=${UPR_FVX_TAG:-vFVX${UPR_FVX_VERSION}}
UPR_FVX_SOURCE_URL=${UPR_FVX_SOURCE_URL:-https://github.com/upr-fvx/universal-pokemon-randomizer-fvx/archive/refs/tags/${UPR_FVX_TAG}.tar.gz}
UPR_FVX_OUTPUT=${UPR_FVX_OUTPUT:-${ROOT_DIR}/vendor/UPR-FVX.jar}
PATCH_DIR=${UPR_FVX_PATCH_DIR:-${ROOT_DIR}/patches/fvx}
WORK_ROOT=${UPR_FVX_WORK_DIR:-${ROOT_DIR}/tmp/build-fvx-$$}
SOURCE_DIR="${WORK_ROOT}/source"
ARCHIVE_PATH="${WORK_ROOT}/source.tar.gz"

cleanup() {
  if [ -z "${UPR_FVX_WORK_DIR:-}" ]; then
    rm -rf "${WORK_ROOT}"
  fi
}
trap cleanup EXIT INT TERM

mkdir -p "${SOURCE_DIR}" "$(dirname -- "${UPR_FVX_OUTPUT}")"

echo "Downloading FVX source ${UPR_FVX_TAG}"
curl -fsSL "${UPR_FVX_SOURCE_URL}" -o "${ARCHIVE_PATH}"

echo "Extracting FVX source"
tar -xzf "${ARCHIVE_PATH}" -C "${SOURCE_DIR}" --strip-components=1

echo "Applying web metadata patch"
cp -R "${PATCH_DIR}/." "${SOURCE_DIR}/"

echo "Building patched FVX JAR"
cd "${SOURCE_DIR}"
sh ./gradlew --no-daemon :random:jar -x test

cp "${SOURCE_DIR}/random/build/libs/UPR-FVX.jar" "${UPR_FVX_OUTPUT}"
echo "Wrote patched FVX JAR to ${UPR_FVX_OUTPUT}"
