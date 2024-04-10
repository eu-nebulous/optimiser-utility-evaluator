# AMPL file for application 'Mercabana Intralogistics' with id 419c5ac2-e8cb-4115-8aa1-27d41ba0a08e

# Variables
var spec_components_0_properties_requests_memory integer >= 1024, <= 8192;
var spec_components_0_traits_0_properties_replicas integer >= 1, <= 8;
var spec_components_0_properties_requests_cpu integer >= 1, <= 8;

# Metrics.  Note that we only emit metrics that are in use.  Values will be provided by the solver.
## Raw metrics
param raw_B;	# raw_B
## Composite metrics
param comp_C;	# comp_C

# Constants
param currentReplicas;

# Performance indicators = composite metrics that have at least one variable in their formula
var pi_F;
var pi_D;
# Performance indicator formulas
subject to define_pi_F : pi_F = pi_D + comp_C;
subject to define_pi_D : pi_D = spec_components_0_traits_0_properties_replicas + comp_C + raw_B;

# TBD: cost parameters - for all components! and use of node-candidates tensor

# Utility functions
maximize firstfunction :
	(comp_C*currentReplicas)/spec_components_0_traits_0_properties_replicas;

# Default utility function: specified in message to solver

# Constraints. For constraints we don't have name from GUI, must be created
