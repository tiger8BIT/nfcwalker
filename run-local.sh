#!/bin/bash
set -e

echo "🔨 Building service with local profile..."
cd /Users/vyacheslavkolodynskiy/IdeaProjects/nfcwalker && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && ./gradlew shadowJar -Plocal --console=plain -x test

echo "🗑️  Removing old containers..."
docker-compose down

echo "🐳 Starting fresh containers..."
docker-compose up --force-recreate "$@"

