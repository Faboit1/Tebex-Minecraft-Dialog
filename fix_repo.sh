sed -i 's|maven("https://repo.opencollab.dev/maven-snapshots/")|maven("https://repo.opencollab.dev/main/")|g' sdk/build.gradle.kts
sed -i 's|maven("https://repo.opencollab.dev/maven-releases/")|maven("https://repo.opencollab.dev/main/")|g' sdk/build.gradle.kts
sed -i 's|maven("https://repo.opencollab.dev/maven-snapshots/")|maven("https://repo.opencollab.dev/main/")|g' build.gradle.kts
sed -i 's|maven("https://repo.opencollab.dev/maven-releases/")|maven("https://repo.opencollab.dev/main/")|g' build.gradle.kts
