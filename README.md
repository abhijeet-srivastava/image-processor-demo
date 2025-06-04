# image-processor-demo
Image Processor Demo Application Documentation

This document provides instructions on how to run the image-processor-demo application and troubleshoot potential issues.

Prerequisites:
    Java: JDK 17 or later
    Maven: Version 3.8.0 or later -  for building the project.
    Docker: Docker Desktop or Docker CLI to run Kafka and the producer.
    Docker Compose: Version 2.0 or later.

Project Structure
The repository contains three main modules:

common:
    A shared library containing common models (ImageProcessingMessage, ProcessStatus)
image-producer:
    A spring-boot application which is used to demo image-processing-consumer, this application
copies image file specified in POST request to input folder and produces a message in kafka topic to trigger
processing in consumer.

image-processing-consumer: 
    A Spring Boot application running locally, which consumes messages from Kafka and processes images in raw image folder.

Setup Instructions:
    1. Build and configure storage volumes-
        mkdir -p ~/Documents/image_processor_demo/input
        mkdir -p ~/Documents/image_processor_demo/raw
    2. Configure these folders as raw an input volumes respectively in docker-compose.yaml
    3. Place a demo image file in file mount for input folder
        cp <path-to-image>/test.jpg ~/Documents/image_processor_demo/input/
    4. Grant permissions to read and copy these file-
        chmod -R u+rw ~/Documents/image_processor_demo

Configure Zookeeper and Kafka:
    Configure docker-compose.yaml to setup docker images to run for Kafka and Zookeeper-
    - Zookeeper on port 2181.
    - Kafka broker on ports 9092 (internal) and 9093 (host).
    - image-producer service connecting to Kafka at kafka:9092.

Build the Application:
    cd image-processing-demo
    Build and Install Common:
        cd common
        mvn clean install
    Build image-producer
        cd ../image-producer
        mvn clean package
    Build image-processing-consumer 
        cd ../image-processing-consumer
        mvn clean package

Start Docker service:
    cd image-processor-demo
    docker-compose up --build -d

Verify containers are running:
    docker ps

Check producer logs:
    docker logs image-producer

Creating Topics for processing , retry and dead-letter-queue-


docker exec -it image-processor-demo-kafka-1 kafka-topics --create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic input-topic \
--config retention.ms=604800000 --config min.cleanable.dirty.ratio=0.9 --config segment.bytes=1048576

docker exec -it image-processor-demo-kafka-1 kafka-topics --create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic retry-input-topic \
--config retention.ms=604800000 --config min.cleanable.dirty.ratio=0.9 --config segment.bytes=1048576

docker exec -it image-processor-demo-kafka-1 kafka-topics --create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic dlq-input-topic \
--config segment.bytes=1048576

Add/Alter config-

docker exec -it image-processor-demo-kafka-1 kafka-configs --bootstrap-server kafka:9092 \
--entity-type topics --entity-name input-topic --alter \
--add-config max.message.bytes=1048576

Run consumer application:
    cd image-processing-consumer
    mvn spring-boot:run

Verify consumer logs:
    tail -f logs/image-processing-consumer.log

Produce a message:
    Trigger the producer to copy an image and send a Kafka message:

    curl --location 'http://localhost:8080/images' \
    --header 'Accept: application/json' \
    --header 'Content-Type: application/json' \
    --data '{
    "file_name": "xray_1.jpg",
    "file_format": "jpg"
    }'
This copies test.jpg from ~/Documents/image_processor_demo/input to ~/Documents/image_processor_demo/raw and sends a message to input-topic.

Troubleshooting Issues:

1. File Not Found in Consumer:
    - Symptoms: Logs show Image file not found
    - Cause: File wasn't copied in raw folder
    - Solution:
      - Verify file exists in input.
      - Check producer logs for copy errors
      - Validate local.baseFilePath in image-producer/respurces/application.properties, is properly configured and matches 
        with input volume mounted in docker-compose.yaml

2. Kafka connection issues:
    - Symptoms: Consumer logs show Connection to node -1 (localhost/127.0.0.1:9093) could not be established.
    - Cause: Kafka broker isn’t running or ports are incorrect.
    - Solution:
      - Verify kafka container is running, and check its logs
        - docker ps | grep image-processor-demo-kafka-1
        - docker logs image-processor-demo-kafka-1
      - Ensure bootstrap-servers=localhost:9093 in application.properties.

3. Messages Stuck in Topic
   - Symptoms: Consumer doesn’t process messages.
   - Cause: Consumer isn’t running, group ID mismatch, or offset issues.
   - Solution:
     - Check consumer group lag
        docker exec -it image-processor-demo-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 \
         --group message-processor-group --describe
     - Reset offset if required-
       docker exec -it image-processor-demo-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 \
       --group message-processor-group --reset-offsets --to-latest --execute --topic input-topic
4. Other Miscelleneous issues-
   a. Producer fails to copy file
        - Verify file exists and appropriate permissions
        - Observe producer logs for any errors
   b. Latest changes for producer or common package not reflecting
        - Ensure common module changes are installed in repo - mvn clean install
        - For producer changes, ensure to package module before building docker image

Alerts:
    Configure alerts for cases which may require manual interventions-
    1. Alerts for Kafka topic offset lag surpassing threshold value
    2. Alerts for any message entering in DLQ topic
    3. Alerts for any particular exception exceeding threshold in particular window.





