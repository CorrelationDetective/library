# Correlation Detective

Correlation Detective is a fast and scalable family of algorithms for finding interesting multivariate correlations in vector datasets. This project provides a versatile tool for exploring and identifying correlations in your data efficiently. Below, we will guide you through the usage of Correlation Detective and its main features.

## Table of Contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
- [Usage](#usage)
    - [Input Format](#input-format)
        - [Accessing input/output data](#accessing-inputoutput-data)
    - [Running the Query](#running-the-query)
    - [Example](#example)
- [License](#license)

## Getting Started

### Installation

1. Fetch the docker image from dockerhub:

    ```bash
    docker pull correlationdetective/correlationdetective
    ```
2. Run the docker image (more information in #Usage):

    ```bash
    docker run -it correlationdetective/correlationdetective <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight> ...
    ```   

## Usage

## Input Format
Correlation Detective expects the input data to be in CSV format, either in row-major or column-major order.

**Row-major format:** When in row-major format, the first row of the CSV file should contain the names of the vectors,
with the first column containing the names of the dimensions.
For example, the following CSV file contains 3 vectors (A, B, C) with 4 dimensions (x, y, z, w):
```csv
,x,y,z,w
A,1,2,3,4
B,5,6,7,8
C,9,10,11,12
```

**Column-major format:** When in column-major format, the first column of the CSV file should contain the names of the vectors,
with the first row containing the names of the dimensions.
For example, the following CSV file contains 3 vectors (A, B, C) with 4 dimensions (x, y, z, w):
```csv
,A,B,C
x,1,5,9
y,2,6,10
z,3,7,11
w,4,8,12
```

### Accessing input/output data
The docker image can access data from either a *mounted volume* hosted on the host machine, or from an object storage bucket, such as AWS S3.

**Mounted volume:**
When using a mounted volume, the data directory on the host machine should be mounted to the container when running the docker image.
For example, if the data directory is located at `/home/user/data`, the docker image should be run as follows:
    ```bash
    docker run -it -v /home/user/data:/data correlationdetective/correlationdetective <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight> ...
    ```
Note that the input and output paths should be the paths to the data directory in the container, in the format `/data/<pathToData>`.

**Object storage bucket:**
When using an object storage bucket, the input and output paths should be the paths to the bucket, in the format `s3://<bucketName>/<pathToData>`.
Additionally, the algorithm will require access to the storage server credentials (e.g., AWS S3) of the user, 
which should be passed to the docker image as environment variables. 
For example, if the data is located on a MinIO server running at http://127.0.0.1:9000 and the access key is `minio` and the secret key is `minio123`, the docker image should be run as follows:
    ```bash
    docker run -it -e MINIO_ENDPOINT_URL=http://127.0.0.1:9000 -e MINIO_ACCESS_KEY=minio -e MINIO_SECRET_KEY=minio123 correlationdetective/correlationdetective <inputPath> <outputPath> <simMetricName> <maxPLeft> <maxPRight> ...
    ```

Note, the library currently supports MinIO as an object storage server. 
Support for AWS S3 and Azure Blob Storage is planned to be released end of 2023.

### Running the Query
To configure and run a specific query on your data with CD, you pass specific parameters when running the docker image.
These optional query parameters should be passed after the required parameters (inputPath, outputPath, simMetricName, maxPLeft, maxPRight) and should be in the following form: 
    ```bash
    --<queryParameterName>=<queryParameterValue>
    ```
OR 
    ```bash
    -<queryParameterName> <queryParameterValue>
    ```
Boolean parameters should be passed as follows:
    ```bash
    -<queryParameterName>
    ```

The list of available query parameters can be found in [PARAMETERS.md](PARAMETERS.md).
We refer to our [paper](https://vldb.org/pvldb/vol15/p1266-papapetrou.pdf) for more details about the parameters and their effects on the algorithm.

### Example
We want to run a *Multipole(4)* *threshold* query with a threshold of 0.85, including the irreducibility constraint.
Our data is located locally at `/home/user/data`.
We run this query as follows:

```bash
docker run -it -v /home/user/data:/data correlationdetective/correlationdetective /data/input_file.csv /data/output_file Multipole 4 0 --queryType=THRESHOLD --tau=0.85 -irreducibility
```

That's it! You are now ready to use Correlation Detective to discover interesting multivariate correlations in your vector datasets.
Explore the project documentation for more details and advanced usage options.

## License
This project is licensed under the GNU GPL v3 License - see the [LICENSE](LICENSE) file for details.
