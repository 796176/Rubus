# Rubus is a protocol for video and audio streaming and
# the client and server reference implementations.
# Copyright (C) 2025 Yegore Vlussove
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

FROM eclipse-temurin:25-noble AS builder
WORKDIR /opt/rubus
RUN apt update && apt install -y maven
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src/main/java/backend src/main/java/backend
COPY src/main/resources src/main/resources
RUN mvn clean package -P server
ARG VERSION
RUN java -Djarmode=tools -jar ./target/RubusServer-$VERSION.jar extract --layers --destination extracted


FROM eclipse-temurin:25-jre-noble AS final
WORKDIR /opt/rubus
ENV RUBUS_WORKING_DIR=/var/opt/rubus
ENV LOCAL_MEDIA=/var/lib/rubus
COPY rubus.conf init.sh ./
RUN chmod o+x init.sh

COPY --from=builder /opt/rubus/extracted/dependencies/ ./
COPY --from=builder /opt/rubus/extracted/spring-boot-loader/ ./
COPY --from=builder /opt/rubus/extracted/snapshot-dependencies/ ./
COPY --from=builder /opt/rubus/extracted/application/ ./

ARG VERSION
ENV VERSION=$VERSION JVM_OPTIONS= DB_USER= DB_PASSWORD=
ENTRYPOINT ["/bin/bash", "-c", \
"./init.sh && java -Drubus.workingDir=$RUBUS_WORKING_DIR -Drubus.db.user=$DB_USER -Drubus.db.password=$DB_PASSWORD \
$JVM_OPTIONS -jar RubusServer-$VERSION.jar"]
