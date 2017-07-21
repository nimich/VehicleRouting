# Vehicle-Routing-Problem

Vehicle Routing Problem or simply VRP is a well known combinatorial optimization problem and a generalization of the travelling salesman problem. A definition of the problem is this: We have a number of customers that have a demand for a delivery. Which are the optimal (minimal) routes for a fleet of vehicles starting from a single point (depot) to deliver in all the customers. Finding optimal solution is NP-hard problem so heurestic strategies are proposed for approximation of the optimal solution.
For more about the problem see: https://en.wikipedia.org/wiki/Vehicle_routing_problem

# Implementation 

The implementation in in Java (built in Java 1.8 but is compatible with at least Java 1.7) and constists of a single runable class named VRP.java.
Three heuristic strategies where tested against a greedy solution. Intra route where a customer can be reassigned in a different position in the same route, inter route where a customer can be reassigned in another position in the all vehicle routes and Tabu search where we keep selecting the best neighboor solution even if it it is worst than the current solution for a number of iterations, to avoid local minimum points.
This code prints the solution from each strategy in console and creates 4 png images for all solutions (Greedy, IntraRoute, InterRoute, Tabu) and 3 files for the evolution in solution costs for the heuristics algorithms. Tabu search has the best perfomance for an instance of the problem where we have 30 random placed customers and 10 vehicles: Greedy solution was 793 distance units (du), Intra Route Heuristic Algorithm gave 761 du , Inter Route 644 du amd finally Tabu Search gave 637 du after 200 iterations. The evolution of the Tabu search solution cost is shown in the next image where we can see that the best solution was found around 20 iteration but this local minimum did not stop the algorithm to try to find a better solutipn.



