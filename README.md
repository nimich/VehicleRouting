# Vehicle-Routing-Problem

  Vehicle Routing Problem or simply VRP is a well known combinatorial optimization problem and a generalization of the travelling salesman problem. A definition of the problem is this: We have a number of customers that have a demand for a delivery. Which are the optimal (minimal) routes for a fleet of vehicles starting from a single point (depot) to deliver the requested goods in all customers. Finding optimal solution is a NP-hard problem so heurestic strategies are proposed for approximation of the optimal solution.
For more about the problem see: https://en.wikipedia.org/wiki/Vehicle_routing_problem

# Implementation 

The implementation in in Java (built in Java 1.8 but is compatible with at least Java 1.7) and was buld in Intellij. The code itself is in a single class named VRP.java.A greedy solution was calculated at first and then three heuristic strategies where tested against it. Intra route where a customer can be reassigned in a different position in the same route, inter route where a customer can be reassigned in another position in the all vehicle routes and Tabu search where we keep selecting the best neighboor solution even if it it is worst than the current solution for a number of iterations.
  
This code prints the solution from each strategy in console and creates 4 png images for all solutions (Greedy, IntraRoute, InterRoute, Tabu) and 3 files for the evolution in solution costs for the heuristics algorithms. Tabu search has the best perfomance; for an instance of the problem where we had 30 random placed customers and 10 vehicles: Greedy solution was 793 distance units (du), Intra Route Heuristic Algorithm gave 761 du , Inter Route 644 du amd finally Tabu Search gave 637 du after 200 iterations. 

Tabu search has the flexibility to overcome local minimum so this is why we expect to be the beter strategy. In the next two images we can see the initial greedy solution graphicxally represented and the final solution the came from Tabu search. 

# Solution Images

Initial greedy solution. We can see that we have many cross edges (edges that are crossed) and are indications that better solutions exist.

![alt text](https://github.com/nimich/VehicleRouting/blob/master/Greedy_Solution.png)

Final solution from TabU search where most of the cross edges have been eliminated.

![alt text](https://github.com/nimich/VehicleRouting/blob/master/TABU_Solution.png)
