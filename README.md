# Correlation Detective

Correlation Detective is a fast and scalable family of algorithms for finding interesting multivariate correlations in vector datasets. This project provides a versatile tool for exploring and identifying correlations in your data efficiently. Below, we will guide you through the usage of Correlation Detective and its main features.

## Table of Contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
        - [Option 1: Install using Maven](#option-1-install-using-maven)
        - [Option 2: Clone the Correlation Detective github repository](#option-2-clone-the-correlation-detective-github-repository)
- [Usage](#usage)
    - [Input Format](#input-format) 
    - [Initializing the CorrelationDetective Object](#initializing-the-correlationdetective-object)
    - [Running the Query](#running-the-query)
    - [Interacting with the ResultSet](#interacting-with-the-resultset)
    - [Accessing Statistics](#accessing-statistics)
    - [Saving Results](#saving-results)
- [Examples](#examples)
- [Documentation](#documentation)
- [License](#license)

## Getting Started

### Prerequisites

Before using Correlation Detective, make sure you have the following prerequisites installed:

- Java 11 or later
- Maven (for building the project)

### Installation

#### Option 1: Install using Maven
1. Add the following dependency to your pom.xml file:

   ```xml
   <dependency>
       <groupId>io.github.correlationdetective</groupId>
       <artifactId>CorrelationDetective</artifactId>
       <version>1.0</version>
   </dependency>
   ```

#### Option 2: Clone the Correlation Detective github repository
1. Clone the Correlation Detective repository to your local machine:

   ```bash
   git clone https://github.com/CorrelationDetective/library.git
    ```
2. Navigate to the project directory:

   ```bash
   cd library
   ```
3. Build the project using Maven:

   ```bash
    mvn clean install
    ```


## Usage
**Note:** The following examples are also implemented as a unit test in src.test.java.library.LibraryUsageTest.java.
You can run this test to see the examples in action.

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

### Initializing the CorrelationDetective Object
To get started with Correlation Detective, you need to initialize a 'CorrelationDetective' object. You can do this in two ways:

### Option 1: Create CorrelationDetective object directly
```java
// Define necessary parameters
String inputPath = "/path/to/your/dataset.csv";
SimEnum simMetricName = SimEnum.PEARSON_CORRELATION;
int maxPLeft = 1;
int maxPRight = 2;

// Create CorrelationDetective object directly
CorrelationDetective cd = new CorrelationDetective(inputPath, simMetricName, maxPLeft, maxPRight);

// Set optional parameters
cd.runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
cd.runParameters.setTau(0.7);
cd.runParameters.setNVectors(200);
```

### Option 2: Create CorrelationDetective object using a RunParameters object
```java
// Define necessary parameters
String inputPath = "/path/to/your/dataset.csv";
SimEnum simMetricName = SimEnum.PEARSON_CORRELATION;
int maxPLeft = 1;
int maxPRight = 2;

// Create a RunParameters object
RunParameters runParameters = RunParameters(inputPath, simMetricName, maxPLeft, maxPRight);

// Set optional parameters
runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
runParameters.setTau(0.7);
runParameters.setNVectors(200);

// Create CorrelationDetective object using RunParameters
CorrelationDetective cd = new CorrelationDetective(runParameters);
```

### Running the Query
Once you have initialized a CorrelationDetective object, you can run the query using the following command:
```java
ResultSet rs = cd.run();
```

### Interacting with the ResultSet
You can interact with the 'ResultSet' object to access the results of your query. Here's how you can get started:
```java
// Get the number of results
int numResults = resultSet.size();
System.out.println("Number of results: " + numResults);

// Get the results as tuples of indexes following the correlation pattern (x, (y, z))
List<ResultTuple> resultTuples = resultSet.getResultTuples();

// Print the first 10 results
System.out.println("First 10 results:");
for (int i = 0; i < 10; i++) {
    ResultTuple resultTuple = resultTuples.get(i);
    System.out.println(resultTuple);
}
```

### Accessing Statistics
To access statistics related to your query, you can use the 'StatBag' object:
```java
// Get the StatBag object
StatBag statBag = cd.getStatBag();

// Print some statistics
System.out.println("Total duration: " + statBag.getTotalDuration());
System.out.println("Total number of cluster combinations inspected: " + statBag.getNCCs());
statBag.printStageDurations();
```

### Saving Results
You can save both the results and statistics as CSV and JSON files for further analysis:
```java
// Define the output directory
String outputDir = "/path/to/output/directory";

// Save results as CSV and JSON
resultSet.saveAsCSV(outputDir + "/results.csv");
resultSet.saveAsJson(outputDir + "/results.json");

// Save StatBag as CSV and JSON
statBag.saveAsCSV(outputDir + "/stats.csv");
statBag.saveAsJson(outputDir + "/stats.json");
```

That's it! You are now ready to use Correlation Detective to discover interesting multivariate correlations in your vector datasets.
Explore the project documentation for more details and advanced usage options.

## Examples
The examples are implemented as unit tests in [LibraryUsageTest](src/test/java/library/LibraryUsageTest.java).
You can run this test to see the examples in action.

## Documentation
### Parameters and Configuration

For detailed information about the parameters and configuration options available in Correlation Detective,
please refer to the [**PARAMETERS.md**](PARAMETERS.md) file.
This document provides an overview of all configurable settings in the `RunParameters` class,
which allows you to fine-tune the behavior of the algorithm for your specific needs.
We refer to our [paper](https://vldb.org/pvldb/vol15/p1266-papapetrou.pdf) for more details about the parameters and their effects on the algorithm.

## License
This project is licensed under the GNU GPL v3 License - see the [LICENSE](LICENSE) file for details.