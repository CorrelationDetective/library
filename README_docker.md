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
   
2. Create the json file with the input parameters. This json should be of the following format:
    
```json
{
    "docket_image": "correlationdetective/correlationdetective:latest",
    "input": [{
        "path": "path/to/input.csv",
        "name": "The name of the file to perform the multivariate correlation analysis on."
    }],
    "parameters": {
        "outputPath": "path/to/output.csv",
        "simMetricName": "pearson_correlation",
        "maxPLeft": 2,
        "maxPRight": 2
    },
    "minio": {
        "id": <minio_access_key>,
        "key": <minio_secret_key>,
        "endpoint_url": <minio_endpoint_url>
    },
    "tags": []
}
```

Note that the `minio` field is optional and should only be included if you are using an object storage bucket to store your data.
Also, the parameters field in the above example only contains the required parameters. 
It can be extended with additional query parameters as described in [PARAMETERS.md](PARAMETERS.md).

3. Run the docker image (more information in #Usage):

    ```bash
    docker run -v path/to/jsons:/app/resources -it correlationdetective/correlationdetective resources/input.json resources/output.json
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

### Running the Query
To configure and run a specific query on your data with CD, you pass specific parameters when running the docker image.
These optional query parameters should be passed as part of the `parameters` field in the input json file. 
The list of available query parameters can be found in [PARAMETERS.md](PARAMETERS.md).
We refer to our [paper](https://vldb.org/pvldb/vol15/p1266-papapetrou.pdf) for more details about the parameters and their effects on the algorithm.

### Example
We want to run a *Multipole(4)* *threshold* query with a threshold of 0.85, including the irreducibility constraint
We run this query with the following input json:

```json
{
    "docket_image": "correlationdetective/correlationdetective:latest",
    "input": [{
        "path": "bucket/data/input.csv",
        "name": "The name of the file to perform the multivariate correlation analysis on."
    }],
    "parameters": {
        "outputPath": "bucket/data/output",
        "simMetricName": "multipole",
        "maxPLeft": 4,
        "maxPRight": 0,
        "tau": 0.85,
        "irreducibility": true
    },
    "minio": {
        "id": "minio",
        "key": "minio123",
        "endpoint_url": "http://localhost:9000"
    },
    "tags": []
}
```

That's it! You are now ready to use Correlation Detective to discover interesting multivariate correlations in your vector datasets.
Explore the project documentation for more details and advanced usage options.

## License
This project is licensed under the GNU GPL v3 License - see the [LICENSE](LICENSE) file for details.