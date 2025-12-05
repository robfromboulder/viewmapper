ARG VERSION
FROM ubuntu:jammy

# copy distribution files to temporary directory
COPY mcp_server.py pyproject.toml README.md viewmapper-478.jar /tmp/runtime/

ARG VERSION
FROM ubuntu:jammy-20251001
ARG VERSION
ENV CONTAINER_VERSION=$VERSION

# configure system packages including Java 24 and Python 3.14
RUN sed -i 's|ports.ubuntu.com|mirrors.ocf.berkeley.edu|g' /etc/apt/sources.list && \
# install base packages needed to add repos \
apt update && apt install --no-install-recommends -y curl gnupg software-properties-common && \
# add deadsnakes repo (for Python 3.14) \
add-apt-repository ppa:deadsnakes/ppa && \
# add Temurin repo (for JRE 24) \
curl -sS https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - && \
echo "deb https://packages.adoptium.net/artifactory/deb jammy main" | tee /etc/apt/sources.list.d/adoptium.list && \
# update package lists after adding both repos \
apt update && \
# install Python and Java \
DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt install --no-install-recommends -y python3.14 python3.14-venv temurin-24-jre && \
# install latest pip version \
curl -sS https://bootstrap.pypa.io/get-pip.py | python3.14 && \
# remove old system Python version (also removes python3 metapackage and symlinks) \
apt remove -y python3.10 && apt -y autoremove && \
# recreate removed python symlinks for convenience \
ln -s /usr/bin/python3.14 /usr/bin/python3 && ln -s /usr/bin/python3.14 /usr/bin/python && \
# remove temp files created when installing packages \
rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /etc/ssl/private/ssl-cert-snakeoil.key

# create runtime user and home directory
RUN addgroup runtime && useradd -g runtime runtime && mkdir -p /home/runtime && chown -R runtime:runtime /home/runtime

# copy distribution files from previous stage
COPY --chown=runtime:runtime --from=0 /tmp/runtime/. /home/runtime/

# switch to runtime user
USER runtime
WORKDIR /home/runtime

# install python packages
RUN python3 -m pip install --no-cache-dir -e .

# run server
ENV VIEWMAPPER_JAR=/home/runtime/viewmapper-478.jar
CMD ["python", "mcp_server.py"]
