ARG BASE_IMAGE=temurin-21-tools-deps-jammy
FROM clojure:${BASE_IMAGE}

SHELL ["/bin/bash", "-ec"]
ENTRYPOINT ["bash"]

# Prepare clojure tools
RUN clojure -Ttools list && \
clojure -Ttools install io.github.seancorfield/clj-new '{:git/tag "v1.2.404" :git/sha "d4a6508"}' :as clj-new && \
clojure -Ttools install-latest :lib io.github.seancorfield/deps-new :as new && \
clojure -Ttools list

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get autoremove -v
