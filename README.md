# Correlation Detective

Correlation Detective is a fast and scalable family of algorithms for finding interesting multivariate correlations in vector datasets. This project provides a versatile tool for exploring and identifying correlations in your data efficiently. Below, we will guide you through the usage of Correlation Detective and its main features.

## Table of Contents

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Usage](#usage)
    - [Initializing the CorrelationDetective Object](#initializing-the-correlationdetective-object)
    - [Running the Query](#running-the-query)
    - [Interacting with the ResultSet](#interacting-with-the-resultset)
    - [Accessing Statistics](#accessing-statistics)
    - [Saving Results](#saving-results)
- [License](#license)

## Getting Started

### Prerequisites

Before using Correlation Detective, make sure you have the following prerequisites installed:

- Java 8 or later
- Maven (for building the project)

### Installation

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

### Initializing the CorrelationDetective Object
To get started with Correlation Detective, you need to initialize a 'CorrelationDetective' object. You can do this in two ways:

### Option 1: Create CorrelationDetective object directly
```java
import src.main.java.algorithms.performance.CorrelationDetective;
import src.main.java.algorithms.performance.RunParameters;
import src.main.java.algorithms.performance.enums.QueryTypeEnum;
import src.main.java.algorithms.performance.enums.SimEnum;

// Define necessary parameters
String inputPath = "/path/to/your/dataset.csv";
SimEnum simMetricName = SimEnum.PEARSON_CORRELATION;
int maxPLeft = 1;
int maxPRight = 2;

// Create CorrelationDetective object directly
CorrelationDetective sd = new CorrelationDetective(inputPath, simMetricName, maxPLeft, maxPRight);

// Set optional parameters
sd.runParameters.setQueryType(QueryTypeEnum.THRESHOLD);
sd.runParameters.setTau(0.7);
sd.runParameters.setNVectors(200);
```

### Option 2: Create CorrelationDetective object using a RunParameters object
```java
import src.main.java.algorithms.performance.CorrelationDetective;
import src.main.java.algorithms.performance.RunParameters;
import src.main.java.algorithms.performance.enums.QueryTypeEnum;
import src.main.java.algorithms.performance.enums.SimEnum;

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
CorrelationDetective sd = new CorrelationDetective(runParameters);
```

### Running the Query
Once you have initialized a CorrelationDetective object, you can run the query using the following command:
```java
ResultSet rs = sd.run();
```

### Interacting with the ResultSet
You can interact with the 'ResultSet' object to access the results of your query. Here's how you can get started:
```java
import src.main.java.algorithms.performance.ResultSet;
import src.main.java.algorithms.performance.ResultTuple;

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
import src.main.java.algorithms.performance.StatBag;

// Get the StatBag object
StatBag statBag = sd.getStatBag();

// Print some statistics
System.out.println("Total duration: " + statBag.getTotalDuration());
System.out.println("Total number of cluster combinations inspected: " + statBag.getNCCs());
statBag.printStageDurations();
```

### Saving Results
You can save both the results and statistics as CSV and JSON files for further analysis:
```java
import src.main.java.algorithms.performance.ResultSet;
import src.main.java.algorithms.performance.StatBag;

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

## License
This project is licensed under the GNU GPL v3 License - see the [LICENSE](LICENSE) file for details.