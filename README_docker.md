# Correlation Detective

Correlation Detective is a fast and scalable family of algorithms for finding interesting multivariate correlations in vector datasets. This project provides a versatile tool for exploring and identifying correlations in your data efficiently. Below, we will guide you through the usage of Correlation Detective and its main features.

## Table of Contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
- [Usage](#usage)
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
    docker run -it correlationdetective/correlationdetective <inputPath> <simMetricName> <maxPLeft> <maxPRight> ...
    ```   

## Usage

### Running the Query
To configure and run a specific query on your data with CD, you pass specific parameters when running the docker image.
These optional query parameters should be passed after the required parameters (inputPath, simMetricName, maxPLeft, maxPRight) and should be in the following form: 
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
We want to run a *Multipole(4)* *threshold* query with a threshold of 0.85, including the irreducibility constraint
We run this query as follows:

```bash
docker run -it correlationdetective/correlationdetective <inputPath> Multipole 4 0 --queryType=THRESHOLD --tau=0.85 -irreducibility
```

That's it! You are now ready to use Correlation Detective to discover interesting multivariate correlations in your vector datasets.
Explore the project documentation for more details and advanced usage options.

## License
This project is licensed under the GNU GPL v3 License - see the [LICENSE](LICENSE) file for details.