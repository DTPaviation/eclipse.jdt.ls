# syntax=docker/dockerfile:1.5.0
FROM eclipse-temurin:17.0.4.1_1-jdk

WORKDIR /etc/jdtls

RUN rm -rf ./*

COPY ./org.eclipse.jdt.ls.product/target/repository .

ENTRYPOINT ["java", "-DCLIENT_HOST=0.0.0.0", "-DCLIENT_PORT=5036", "-Declipse.application=org.eclipse.jdt.ls.core.id1", "-Dosgi.bundles.defaultStartLevel=4", "-Declipse.product=org.eclipse.jdt.ls.core.product", "-Dlog.level=ALL", "-noverify", "-Xmx2G", "--add-modules=ALL-SYSTEM", "--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/etc/jdtls/plugins/org.eclipse.equinox.launcher_Latest.jar", "-configuration", "./config_linux", "-data", "/etc/jdtls/workspace"]
