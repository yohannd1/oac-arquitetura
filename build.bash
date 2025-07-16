#!/usr/bin/env bash

srcDir="src"
buildDir="bin"
depsDir="deps"

set -ue
progname=$(basename "$0")

showHelp() {
  printf >&2 "Usage: %s { build | run <PROGRAM> | test | clean }\n" "$progname"
  exit 2
}

[ $# = 0 ] && showHelp || true

makeClassPath() {
  printf "%s:" "$buildDir" "$depsDir"/*.jar
  printf "."
}

mkdir -p "$buildDir"
mkdir -p "$depsDir"

run() {
  printf >&2 "%s\n" "$*"
  "$@"
}

downloadIfNotExist() {
  [ $# = 2 ] || exit 2

  if [ ! -e "$1" ]; then
    curl -L --output "$1" -- "$2"
  fi
}

doBuild() {
  sources=("$srcDir"/**/*.java "$srcDir"/*.java)
  cflags=(-Xlint:unchecked)

  downloadIfNotExist "$depsDir/junit.jar" \
    "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar"

  downloadIfNotExist "$depsDir/junit-platform-console-standalone.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.13.3/junit-platform-console-standalone-1.13.3.jar"

  downloadIfNotExist "$depsDir/junit-jupiter-engine.0.0-M1.jar" \
    "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/6.0.0-M1/junit-jupiter-engine-6.0.0-M1.jar"

  downloadIfNotExist "$depsDir/junit-jupiter-api.0.0-M1.jar" \
    "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/6.0.0-M1/junit-jupiter-api-6.0.0-M1.jar"

  classPath=$(makeClassPath)
  run javac -d "$buildDir" -cp "$classPath" "${cflags[@]}" "${sources[@]}"
}

doClean() {
  if [ -r "$buildDir" ]; then rm -r "$buildDir"; fi
  if [ -r "$depsDir" ]; then rm -r "$depsDir"; fi
}

doRun() {
  doBuild
  classPath=$(makeClassPath)

  run java -cp "$classPath" assembler.Assembler "$1"
  run java -cp "$classPath" architecture.Architecture "$1"
}

doTest() {
  doBuild

  run java -cp "$classPath" org.junit.platform.console.ConsoleLauncher execute --scan-classpath
}

case "$1" in
  build)
    [ $# = 1 ] || showHelp
    doBuild
    ;;
  clean)
    [ $# = 1 ] || showHelp
    doClean
    ;;
  run)
    [ $# = 2 ] || showHelp
    shift
    doRun "$@"
    ;;
  test)
    [ $# = 1 ] || showHelp
    doTest
    ;;
  *) showHelp ;;
esac
