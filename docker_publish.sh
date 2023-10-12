#!/bin/bash

mvn clean package -DskipTests

docker login

docker build -t correlationdetective/correlationdetective:latest .

docker push correlationdetective/correlationdetective:latest