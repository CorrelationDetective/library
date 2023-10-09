Instructions on how to push a new version of the docker image to the CD dockerhub:
0. Make sure the jar file is up to date:
    ```bash
    mvn clean package
    ```
1. Login to the correlationdetective dockerhub account (find credentials in Bitwarden):
    ```bash
    docker login
    ```
2. Build the docker image locally:
    ```bash
    docker build -t correlationdetective/correlationdetective .
    ```
3Push the docker image to dockerhub:
    ```bash
    docker push correlationdetective/correlationdetective
    ```