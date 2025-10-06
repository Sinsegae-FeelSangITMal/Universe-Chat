#!/bin/sh
#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# gradle startup script for UN*X
#

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done

APP_HOME=`dirname "$PRG"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
darwin=false
linux=false
case "`uname`" in
    CYGWIN*) 
        cygwin=true
        ;;
    Darwin*) 
        darwin=true
        ;;
    Linux) 
        linux=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to find JAVA_HOME
if [ -z "$JAVA_HOME" ] ; then
    if $darwin ; then
        if [ -x '/usr/libexec/java_home' ] ; then
            JAVA_HOME=`/usr/libexec/java_home`
        elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
            JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
        fi
    else
        javaPath="`which java 2>/dev/null`"
        if [ -n "$javaPath" ] ; then
            javaPath="`readlink -f "$javaPath" 2>/dev/null`"
            JAVA_HOME="`dirname "$javaPath" 2>/dev/null`"
            JAVA_HOME="`dirname "$JAVA_HOME" 2>/dev/null`"
        fi
    fi
fi

# If we still don't have a JAVA_HOME, try to find a JDK
if [ -z "$JAVA_HOME" ] ; then
    # Guess JAVA_HOME
    if [ -d "/usr/lib/jvm/java-11-openjdk" ] ; then
        JAVA_HOME="/usr/lib/jvm/java-11-openjdk"
    elif [ -d "/usr/lib/jvm/java-8-openjdk" ] ; then
        JAVA_HOME="/usr/lib/jvm/java-8-openjdk"
    elif [ -d "/usr/lib/jvm/java-1.8.0-openjdk" ] ; then
        JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk"
    fi
fi

# If we still don't have a JAVA_HOME, we can't go on
if [ -z "$JAVA_HOME" ] ; then
    die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
fi

# Set JAVA_EXE
JAVA_EXE="$JAVA_HOME/bin/java"

if [ ! -x "$JAVA_EXE" ] ; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME\n\nPlease set the JAVA_HOME variable in your environment to match the\nlocation of your Java installation."
fi

# Set APP_NAME
APP_NAME="gradlew"

# Set APP_BASE_NAME
APP_BASE_NAME=`basename "$0"`

# Set the GRADLE_HOME
GRADLE_HOME="$APP_HOME/gradle"

# Set the classpath
GRADLE_WRAPPER_JAR="$GRADLE_HOME/wrapper/gradle-wrapper.jar"

# Process the options
if [ "$1" = "--stop" ] ; then
    "$JAVA_EXE" \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$GRADLE_WRAPPER_JAR" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"
    exit $?
elif [ "$1" = "-d" ] || [ "$1" = "--daemon" ] ; then
    # Check for a running daemon
    if ! "$JAVA_EXE" \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$GRADLE_WRAPPER_JAR" \
        org.gradle.wrapper.GradleWrapperMain \
        --status > /dev/null 2>&1 ; then
        # No daemon running, so start one
        "$JAVA_EXE" \
            "-Dorg.gradle.appname=$APP_BASE_NAME" \
            -classpath "$GRADLE_WRAPPER_JAR" \
            org.gradle.wrapper.GradleWrapperMain \
            "$@"
        exit $?
    fi
elif [ "$1" = "--no-daemon" ] ; then
    # Stop any running daemons
    "$JAVA_EXE" \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$GRADLE_WRAPPER_JAR" \
        org.gradle.wrapper.GradleWrapperMain \
        --stop > /dev/null 2>&1
fi

# Split up the JVM options only if DEFAULT_JVM_OPTS is not null
if [ -n "$DEFAULT_JVM_OPTS" ] ; then
    eval jvm_options=($DEFAULT_JVM_OPTS)
fi

# Execute Gradle
exec "$JAVA_EXE" \
    "${jvm_options[@]}" \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$GRADLE_WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"