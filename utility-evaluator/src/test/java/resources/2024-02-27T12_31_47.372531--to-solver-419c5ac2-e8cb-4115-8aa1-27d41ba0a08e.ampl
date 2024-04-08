# AMPL file for application 'Mercabana Intralogistics' with id 419c5ac2-e8cb-4115-8aa1-27d41ba0a08e

# Variables
var spec_components_0_traits_0_properties_replicas integer >= 1, <= 8;

# Metrics.  Note that we only emit metrics that are in use.  Values will be provided by the solver.
## Raw metrics
## Composite metrics
param mean_job_process_time;	# mean_job_process_time

# Constants
param currentReplicas;

# Performance indicators = composite metrics that have at least one variable in their formula
# Performance indicator formulas

# TBD: cost parameters - for all components! and use of node-candidates tensor

# Utility functions
maximize utilityfunction :
	-1*((mean_job_process_time*currentReplicas)/spec_components_0_traits_0_properties_replicas);

# Default utility function: specified in message to solver

# Constraints. For constraints we don't have name from GUI, must be created
