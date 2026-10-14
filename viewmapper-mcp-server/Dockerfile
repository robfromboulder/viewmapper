ARG VERSION
FROM ubuntu:resolute-20260927

# copy distribution files to temporary directory
COPY mcp_server.py pyproject.toml README.md viewmapper-483.jar /tmp/runtime/

ARG VERSION
FROM ubuntu:resolute-20260927
ARG VERSION
ENV CONTAINER_VERSION=$VERSION

# configure system packages including Java 25 and Python 3.14, both from the Ubuntu archive
RUN sed -i 's|ports.ubuntu.com|mirrors.ocf.berkeley.edu|g' /etc/apt/sources.list.d/ubuntu.sources && \
apt update && \
DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt install --no-install-recommends -y python3.14 python3.14-venv openjdk-25-jre-headless && \
# remove temp files created when installing packages \
rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# create runtime user and home directory
RUN groupadd runtime && useradd -m -g runtime runtime

# copy distribution files from previous stage
COPY --chown=runtime:runtime --from=0 /tmp/runtime/. /home/runtime/

# switch to runtime user
USER runtime
WORKDIR /home/runtime

# install python packages into a venv, since Ubuntu blocks pip from changing the system Python
RUN python3.14 -m venv venv && venv/bin/pip install --no-cache-dir -e .

# run server
ENV VIEWMAPPER_JAR=/home/runtime/viewmapper-483.jar
CMD ["venv/bin/python", "mcp_server.py"]
