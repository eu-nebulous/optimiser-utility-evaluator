# Optimiser Utility Evaluator

The Utility Evaluator is a Java-based component designed to estimate performance indicator functions. Currently, its functionality is focused on estimating the price parameters for the deployed components.


## Current Flow of Actions

### Messages Received
1. **DSL Generic Message**: Contains all information needed for application deployment.
   - Example: `{"type": "dsl.generic", "content": { ... }}`

### Messages Sent
1. **Performance Indicator Message**: Contains the performance indicators generated from the evaluation.
   - Example: `{"type": "performanceIndicator", "performanceIndicators": { ... }, "initialDataFile": "..." }`
2. **Error Message**: Sent if there is an error during the evaluation process.
   - Example: `{"type": "error", "message": "Error details"}`

### Detailed Flow
1. **Initialization**: The `UtilityEvaluatorController` is initialized with the necessary context and publisher.
2. **Message Reception**: The `DslGenericMessageHandler` listens for incoming DSL generic messages on `eu.nebulouscloud.ui.dsl.generic.>`.
3. **Processing**:
   - Upon receiving a DSL generic message, the `DslGenericMessageHandler` parses the message into an `Application` object.
   - The `UtilityEvaluatorController` processes the application to create initial cost performance indicators.
     - For each component in the application:
       - Fetch node candidates using `NodeCandidatesFetchingService`.
       - Convert node candidates to DTOs using `NodeCandidateConverter`.
       - Create a regression object (`SimpleCostRegression`) if the component has relevant variables (CPU, RAM).
       - Save the regression object in the application.
       - Send the performance indicators using `PerformanceIndicatorSendingService`.
4. **Message Sending**: The results are sent back as a performance indicator message on the topic `eu.nebulouscloud.optimiser.utilityevaluator.performanceindicators`. If an error occurs, an error message is sent.

### NodeCandidatesFetchingService
The `NodeCandidatesFetchingService` is responsible for fetching node candidates from the SAL (Service Abstraction Layer) via the EXN middleware. It performs the following steps:

1. **Generate Requirements**: Based on the `kubevela` configuration in the `Application` object, it generates the requirements for node candidates. Currently, there are no requirements being passed so that all available Node Candidates are used to train the regression function.
2. **Send Request**: Sends a request to the SAL via the EXN middleware to fetch node candidates that match the requirements.
3. **Receive Response**: Receives the response from the SAL, which contains the list of node candidates.
4. **Parse Response**: Parses the response to extract the node candidates and convert them into a list of `NodeCandidate` objects.

### Output: Performance Indicators Message
The initial data file message sent on `eu.nebulouscloud.optimiser.utilityevaluator.performanceindicators` contains the initial coefficients for the regression models. The format is as follows:

```json
{
    "type": "performanceIndicator",
    "performanceIndicators": [
        {
            "name": "cost_pi_component1",
            "variables": ["cpu1", "ram1"],
            "coefficientsName": "COEFFICIENTS_component1"
        },
        {
            "name": "cost_pi_component2",
            "variables": ["cpu2"],
            "coefficientsName": "COEFFICIENTS_component2"
        }
    ],
    "initialDataFile": "param: COEFFICIENTS_component1 := 1 0.1 2 0.3; param: COEFFICIENTS_component2 := 1 0.1;"
}
```

- **performanceIndicators**: An array of performance indicators, each containing:
  - **name**: The name of the performance indicator.
  - **variables**: The list of variables used in the regression model.
  - **coefficientsName**: The name of the coefficients set for the regression model.
- **initialDataFile**: A string containing the initial coefficients for each performance indicator in the format `param: COEFFICIENTS_<component> := <index> <value> ...;`.

## Functionalities for Second Release
- Add latitude and longitude to the available regression parameters.
- Create Node Candidates tensor

