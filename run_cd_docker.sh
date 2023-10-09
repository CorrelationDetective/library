#!/usr/bin/bash

sudo docker build -t correlation-detective . &&

inputPath=s3://correlation-detective/example_data.csv
outputPath=s3://correlation-detective
simMetricName=PEARSON_CORRELATION
maxPLeft=1
maxPRight=2

#sudo docker run  \
#-v /home/jens/tue/1.SimilarityDetective/SimilarityDetective/docker_mount:/app/data \
#correlation-detective \
#$inputPath $outputPath $simMetricName $maxPLeft $maxPRight

sudo docker run -it \
-e MINIO_ENDPOINT_URL="http://localhost:9000" \
-e MINIO_ACCESS_KEY="minioadmin" \
-e MINIO_SECRET_KEY="minioadmin" \
--network="host" \
correlation-detective \
$inputPath $outputPath $simMetricName $maxPLeft $maxPRight

exit 0