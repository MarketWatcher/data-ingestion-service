FROM java:8

COPY target/universal/data-ingestion-service-1.0.zip /opt/ingestion.zip

WORKDIR /opt

RUN unzip ingestion.zip

ENTRYPOINT ["/opt/data-ingestion-service-1.0/bin/data-ingestion-service"]