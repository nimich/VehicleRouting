/*
 * Created by Nikolaos Michail
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

public class  VRP{

    public static void main(String[] args) {

        Random ran = new Random(151190);

        //Problem Parameters
        int NoOfCustomers = 30;
        int NoOfVehicles = 10;
        int VehicleCap = 50;

        //Depot Coordinates
        int Depot_x = 50;
        int Depot_y = 50;

        //Tabu Parameter
        int TABU_Horizon = 10;

        //Initialise
        //Create Random Customers
        Node[] Nodes = new Node[NoOfCustomers + 1];
        Node depot = new Node(Depot_x, Depot_y);

        Nodes[0] = depot;
        for (int i = 1; i <= NoOfCustomers; i++) {
            Nodes[i] = new Node(i, //Id ) is reserved for depot
                    ran.nextInt(100), //Random Cordinates
                    ran.nextInt(100),
                    4 + ran.nextInt(7)  //Random Demand
            );
        }

        double[][] distanceMatrix = new double[NoOfCustomers + 1][NoOfCustomers + 1];
        double Delta_x, Delta_y;
        for (int i = 0; i <= NoOfCustomers; i++) {
            for (int j = i + 1; j <= NoOfCustomers; j++) //The table is summetric to the first diagonal
            {                                      //Use this to compute distances in O(n/2)

                Delta_x = (Nodes[i].Node_X - Nodes[j].Node_X);
                Delta_y = (Nodes[i].Node_Y - Nodes[j].Node_Y);

                double distance = Math.sqrt((Delta_x * Delta_x) + (Delta_y * Delta_y));

                distance = Math.round(distance);                //Distance is Casted in Integer
                //distance = Math.round(distance*100.0)/100.0; //Distance in double

                distanceMatrix[i][j] = distance;
                distanceMatrix[j][i] = distance;
            }
        }
        int printMatrix = 0; //If we want to print diastance matrix

        if (printMatrix == 1){
            for (int i = 0; i <= NoOfCustomers; i++) {
                for (int j = 0; j <= NoOfCustomers; j++) {
                    System.out.print(distanceMatrix[i][j] + "  ");
                }
                System.out.println();
            }
        }

        //Compute the greedy Solution
        System.out.println("Attempting to resolve Vehicle Routing Problem (VRP) for "+NoOfCustomers+
                " Customers and "+NoOfVehicles+" Vehicles"+" with "+VehicleCap + " units of capacity\n");

        Solution s = new Solution(NoOfCustomers, NoOfVehicles, VehicleCap);

        s.GreedySolution(Nodes, distanceMatrix);

        s.SolutionPrint("Greedy Solution");

        draw.drawRoutes(s, "Greedy_Solution");

        s.IntraRouteLocalSearch(Nodes, distanceMatrix);

        s.SolutionPrint("Solution after Intra-Route Heuristic Neighborhood Search");

        draw.drawRoutes(s, "Intra-Route");

        s.GreedySolution(Nodes, distanceMatrix);

        s.InterRouteLocalSearch(Nodes, distanceMatrix);

        s.SolutionPrint("Solution after Inter-Route Heuristic Neighborhood Search");

        draw.drawRoutes(s, "Inter-Route");

        s.GreedySolution(Nodes, distanceMatrix);

        s.TabuSearch(TABU_Horizon, distanceMatrix);

        s.SolutionPrint("Solution After Tabu Search");

        draw.drawRoutes(s, "TABU_Solution");
    }
}

class Solution
{
    int NoOfVehicles;
    int NoOfCustomers;
    Vehicle[] Vehicles;
    double Cost;

    //Tabu Variables
    public Vehicle[] VehiclesForBestSolution;
    double BestSolutionCost;

    public ArrayList<Double> PastSolutions;

    Solution(int CustNum, int VechNum , int VechCap)
    {
        this.NoOfVehicles = VechNum;
        this.NoOfCustomers = CustNum;
        this.Cost = 0;
        Vehicles = new Vehicle[NoOfVehicles];
        VehiclesForBestSolution =  new Vehicle[NoOfVehicles];
        PastSolutions = new ArrayList<>();

        for (int i = 0 ; i < NoOfVehicles; i++)
        {
            Vehicles[i] = new Vehicle(i+1,VechCap);
            VehiclesForBestSolution[i] = new Vehicle(i+1,VechCap);
        }
    }

    public boolean UnassignedCustomerExists(Node[] Nodes)
    {
        for (int i = 1; i < Nodes.length; i++)
        {
            if (!Nodes[i].IsRouted)
                return true;
        }
        return false;
    }

    public void GreedySolution(Node[] Nodes , double[][] CostMatrix) {

        double CandCost,EndCost;
        int VehIndex = 0;

        while (UnassignedCustomerExists(Nodes)) {

            int CustIndex = 0;
            Node Candidate = null;
            double minCost = (float) Double.MAX_VALUE;

            if (Vehicles[VehIndex].Route.isEmpty())
            {
                Vehicles[VehIndex].AddNode(Nodes[0]);
            }

            for (int i = 1; i <= NoOfCustomers; i++) {
                if (Nodes[i].IsRouted == false) {
                    if (Vehicles[VehIndex].CheckIfFits(Nodes[i].demand)) {
                        CandCost = CostMatrix[Vehicles[VehIndex].CurLoc][i];
                        if (minCost > CandCost) {
                            minCost = CandCost;
                            CustIndex = i;
                            Candidate = Nodes[i];
                        }
                    }
                }
            }

            if ( Candidate  == null)
            {
                //Not a single Customer Fits
                if ( VehIndex+1 < Vehicles.length ) //We have more vehicles to assign
                {
                    if (Vehicles[VehIndex].CurLoc != 0) {//End this route
                        EndCost = CostMatrix[Vehicles[VehIndex].CurLoc][0];
                        Vehicles[VehIndex].AddNode(Nodes[0]);
                        this.Cost +=  EndCost;
                    }
                    VehIndex = VehIndex+1; //Go to next Vehicle
                }
                else //We DO NOT have any more vehicle to assign. The problem is unsolved under these parameters
                {
                    System.out.println("\nThe rest customers do not fit in any Vehicle\n" +
                            "The problem cannot be resolved under these constrains");
                    System.exit(0);
                }
            }
            else
            {
                Vehicles[VehIndex].AddNode(Candidate);//If a fitting Customer is Found
                Nodes[CustIndex].IsRouted = true;
                this.Cost += minCost;
            }
        }

        EndCost = CostMatrix[Vehicles[VehIndex].CurLoc][0];
        Vehicles[VehIndex].AddNode(Nodes[0]);
        this.Cost +=  EndCost;

    }


    public void TabuSearch(int TABU_Horizon, double[][] CostMatrix) {

        //We use 1-0 exchange move
        ArrayList<Node> RouteFrom;
        ArrayList<Node> RouteTo;

        int MovingNodeDemand = 0;

        int VehIndexFrom,VehIndexTo;
        double BestNCost,NeigthboorCost;

        int SwapIndexA = -1, SwapIndexB = -1, SwapRouteFrom =-1, SwapRouteTo=-1;

        int MAX_ITERATIONS = 200;
        int iteration_number= 0;

        int DimensionCustomer = CostMatrix[1].length;
        int TABU_Matrix[][] = new int[DimensionCustomer+1][DimensionCustomer+1];

        BestSolutionCost = this.Cost; //Initial Solution Cost

        boolean Termination = false;

        while (!Termination)
        {
            iteration_number++;
            BestNCost = Double.MAX_VALUE;

            for (VehIndexFrom = 0;  VehIndexFrom <  this.Vehicles.length;  VehIndexFrom++) {
                RouteFrom =  this.Vehicles[VehIndexFrom].Route;
                int RoutFromLength = RouteFrom.size();
                for (int i = 1; i < RoutFromLength - 1; i++) { //Not possible to move depot!

                    for (VehIndexTo = 0; VehIndexTo <  this.Vehicles.length; VehIndexTo++) {
                        RouteTo =   this.Vehicles[VehIndexTo].Route;
                        int RouteTolength = RouteTo.size();
                        for (int j = 0; (j < RouteTolength - 1); j++) {//Not possible to move after last Depot!

                            MovingNodeDemand = RouteFrom.get(i).demand;

                            if ((VehIndexFrom == VehIndexTo) ||  this.Vehicles[VehIndexTo].CheckIfFits(MovingNodeDemand)) {
                                //If we assign to a different route check capacity constrains
                                //if in the new route is the same no need to check for capacity

                                if (((VehIndexFrom == VehIndexTo) && ((j == i) || (j == i - 1))) == false)  // Not a move that Changes solution cost
                                {
                                    double MinusCost1 = CostMatrix[RouteFrom.get(i - 1).NodeId][RouteFrom.get(i).NodeId];
                                    double MinusCost2 = CostMatrix[RouteFrom.get(i).NodeId][RouteFrom.get(i + 1).NodeId];
                                    double MinusCost3 = CostMatrix[RouteTo.get(j).NodeId][RouteTo.get(j + 1).NodeId];

                                    double AddedCost1 = CostMatrix[RouteFrom.get(i - 1).NodeId][RouteFrom.get(i + 1).NodeId];
                                    double AddedCost2 = CostMatrix[RouteTo.get(j).NodeId][RouteFrom.get(i).NodeId];
                                    double AddedCost3 = CostMatrix[RouteFrom.get(i).NodeId][RouteTo.get(j + 1).NodeId];

                                    //Check if the move is a Tabu! - If it is Tabu break
                                    if ((TABU_Matrix[RouteFrom.get(i - 1).NodeId][RouteFrom.get(i+1).NodeId] != 0)
                                            || (TABU_Matrix[RouteTo.get(j).NodeId][RouteFrom.get(i).NodeId] != 0)
                                            || (TABU_Matrix[RouteFrom.get(i).NodeId][RouteTo.get(j+1).NodeId] != 0)) {
                                        break;
                                    }

                                    NeigthboorCost = AddedCost1 + AddedCost2 + AddedCost3
                                            - MinusCost1 - MinusCost2 - MinusCost3;

                                    if (NeigthboorCost < BestNCost) {
                                        BestNCost = NeigthboorCost;
                                        SwapIndexA = i;
                                        SwapIndexB = j;
                                        SwapRouteFrom = VehIndexFrom;
                                        SwapRouteTo = VehIndexTo;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int o = 0; o < TABU_Matrix[0].length;  o++) {
                for (int p = 0; p < TABU_Matrix[0].length ; p++) {
                    if (TABU_Matrix[o][p] > 0)
                    { TABU_Matrix[o][p]--; }
                }
            }

            RouteFrom =  this.Vehicles[SwapRouteFrom].Route;
            RouteTo =  this.Vehicles[SwapRouteTo].Route;
            this.Vehicles[SwapRouteFrom].Route = null;
            this.Vehicles[SwapRouteTo].Route = null;

            Node SwapNode = RouteFrom.get(SwapIndexA);

            int NodeIDBefore = RouteFrom.get(SwapIndexA-1).NodeId;
            int NodeIDAfter = RouteFrom.get(SwapIndexA+1).NodeId;
            int NodeID_F = RouteTo.get(SwapIndexB).NodeId;
            int NodeID_G = RouteTo.get(SwapIndexB+1).NodeId;

            Random TabuRan = new Random();
            int RendomDelay1 = TabuRan.nextInt(5);
            int RendomDelay2 = TabuRan.nextInt(5);
            int RendomDelay3 = TabuRan.nextInt(5);

            TABU_Matrix[NodeIDBefore][SwapNode.NodeId] = TABU_Horizon + RendomDelay1;
            TABU_Matrix[SwapNode.NodeId][NodeIDAfter]  = TABU_Horizon + RendomDelay2 ;
            TABU_Matrix[NodeID_F][NodeID_G] = TABU_Horizon + RendomDelay3;

            RouteFrom.remove(SwapIndexA);

            if (SwapRouteFrom == SwapRouteTo) {
                if (SwapIndexA < SwapIndexB) {
                    RouteTo.add(SwapIndexB, SwapNode);
                } else {
                    RouteTo.add(SwapIndexB + 1, SwapNode);
                }
            }
            else
            {
                RouteTo.add(SwapIndexB+1, SwapNode);
            }


            this.Vehicles[SwapRouteFrom].Route = RouteFrom;
            this.Vehicles[SwapRouteFrom].load -= MovingNodeDemand;

            this.Vehicles[SwapRouteTo].Route = RouteTo;
            this.Vehicles[SwapRouteTo].load += MovingNodeDemand;

            PastSolutions.add(this.Cost);

            this.Cost  += BestNCost;

            if (this.Cost <   BestSolutionCost)
            {
                SaveBestSolution();
            }

            if (iteration_number == MAX_ITERATIONS)
            {
                Termination = true;
            }
        }

        this.Vehicles = VehiclesForBestSolution;
        this.Cost = BestSolutionCost;

        try{
            PrintWriter writer = new PrintWriter("PastSolutionsTabu.txt", "UTF-8");
            writer.println("Solutions"+"\t");
            for  (int i = 0; i< PastSolutions.size(); i++){
                writer.println(PastSolutions.get(i)+"\t");
            }
            writer.close();
        } catch (Exception e) {}
    }

    public void SaveBestSolution()
    {
        BestSolutionCost = Cost;
        for (int j=0 ; j < NoOfVehicles ; j++)
        {
            VehiclesForBestSolution[j].Route.clear();
            if (! Vehicles[j].Route.isEmpty())
            {
                int RoutSize = Vehicles[j].Route.size();
                for (int k = 0; k < RoutSize ; k++) {
                    Node n = Vehicles[j].Route.get(k);
                    VehiclesForBestSolution[j].Route.add(n);
                }
            }
        }
    }


    public void InterRouteLocalSearch(Node[] Nodes,  double[][] CostMatrix) {

        //We use 1-0 exchange move
        ArrayList<Node> RouteFrom;
        ArrayList<Node> RouteTo;

        int MovingNodeDemand = 0;

        int VehIndexFrom,VehIndexTo;
        double BestNCost,NeigthboorCost;

        int SwapIndexA = -1, SwapIndexB = -1, SwapRouteFrom =-1, SwapRouteTo=-1;

        int MAX_ITERATIONS = 1000000;
        int iteration_number= 0;

        boolean Termination = false;

        while (!Termination)
        {
            iteration_number++;
            BestNCost = Double.MAX_VALUE;

            for (VehIndexFrom = 0;  VehIndexFrom < this.Vehicles.length;  VehIndexFrom++) {
                RouteFrom = this.Vehicles[VehIndexFrom].Route;
                int RoutFromLength = RouteFrom.size();
                for (int i = 1; i < RoutFromLength - 1; i++) { //Not possible to move depot!

                    for (VehIndexTo = 0; VehIndexTo < this.Vehicles.length; VehIndexTo++) {
                        RouteTo =  this.Vehicles[VehIndexTo].Route;
                        int RouteTolength = RouteTo.size();
                        for (int j = 0; (j < RouteTolength - 1); j++) {//Not possible to move after last Depot!

                            MovingNodeDemand = RouteFrom.get(i).demand;
                            if ( (VehIndexFrom == VehIndexTo) ||  this.Vehicles[VehIndexTo].CheckIfFits(MovingNodeDemand) )
                            {
                                if (( (VehIndexFrom == VehIndexTo) && ((j == i) || (j == i - 1)) ) == false)  // Not a move that Changes solution cost
                                {
                                    double MinusCost1 = CostMatrix[RouteFrom.get(i - 1).NodeId][RouteFrom.get(i).NodeId];
                                    double MinusCost2 = CostMatrix[RouteFrom.get(i).NodeId][RouteFrom.get(i + 1).NodeId];
                                    double MinusCost3 = CostMatrix[RouteTo.get(j).NodeId][RouteTo.get(j + 1).NodeId];

                                    double AddedCost1 = CostMatrix[RouteFrom.get(i - 1).NodeId][RouteFrom.get(i + 1).NodeId];
                                    double AddedCost2 = CostMatrix[RouteTo.get(j).NodeId][RouteFrom.get(i).NodeId];
                                    double AddedCost3 = CostMatrix[RouteFrom.get(i).NodeId][RouteTo.get(j + 1).NodeId];

                                    NeigthboorCost = AddedCost1 + AddedCost2 + AddedCost3
                                            - MinusCost1 - MinusCost2 - MinusCost3;

                                    if (NeigthboorCost < BestNCost) {
                                        BestNCost = NeigthboorCost;
                                        SwapIndexA = i;
                                        SwapIndexB = j;
                                        SwapRouteFrom = VehIndexFrom;
                                        SwapRouteTo = VehIndexTo;

                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (BestNCost < 0) {// If Best Neightboor Cost is better than the current

                RouteFrom = this.Vehicles[SwapRouteFrom].Route;
                RouteTo = this.Vehicles[SwapRouteTo].Route;
                this.Vehicles[SwapRouteFrom].Route = null;
                this.Vehicles[SwapRouteTo].Route = null;

                Node SwapNode = RouteFrom.get(SwapIndexA);

                RouteFrom.remove(SwapIndexA);

                if (SwapRouteFrom == SwapRouteTo) {
                    if (SwapIndexA < SwapIndexB) {
                        RouteTo.add(SwapIndexB, SwapNode);
                    } else {
                        RouteTo.add(SwapIndexB + 1, SwapNode);
                    }
                }
                else
                {
                    RouteTo.add(SwapIndexB+1, SwapNode);
                }

                this.Vehicles[SwapRouteFrom].Route = RouteFrom;
                this.Vehicles[SwapRouteFrom].load -= MovingNodeDemand;

                this.Vehicles[SwapRouteTo].Route = RouteTo;
                this.Vehicles[SwapRouteTo].load += MovingNodeDemand;

                PastSolutions.add(this.Cost);
                this.Cost  += BestNCost;
            }
            else{
                Termination = true;
            }

            if (iteration_number == MAX_ITERATIONS)
            {
                Termination = true;
            }
        }
        PastSolutions.add(this.Cost);

        try{
            PrintWriter writer = new PrintWriter("PastSolutionsInter.txt", "UTF-8");
            for  (int i = 0; i< PastSolutions.size(); i++){
                writer.println(PastSolutions.get(i)+"\t");
            }
            writer.close();
        } catch (Exception e) {}
    }

    public void IntraRouteLocalSearch(Node[] Nodes,  double[][] CostMatrix) {

        //We use 1-0 exchange move
        ArrayList<Node> rt;
        double BestNCost,NeigthboorCost;

        int SwapIndexA = -1, SwapIndexB = -1, SwapRoute =-1;

        int MAX_ITERATIONS = 1000000;
        int iteration_number= 0;

        boolean Termination = false;

        while (!Termination)
        {
            iteration_number++;
            BestNCost = Double.MAX_VALUE;

            for (int VehIndex = 0; VehIndex < this.Vehicles.length; VehIndex++) {
                rt = this.Vehicles[VehIndex].Route;
                int RoutLength = rt.size();

                for (int i = 1; i < RoutLength - 1; i++) { //Not possible to move depot!

                    for (int j =  0 ; (j < RoutLength-1); j++) {//Not possible to move after last Depot!

                        if ( ( j != i ) && (j != i-1) ) { // Not a move that cHanges solution cost

                            double MinusCost1 = CostMatrix[rt.get(i-1).NodeId][rt.get(i).NodeId];
                            double MinusCost2 =  CostMatrix[rt.get(i).NodeId][rt.get(i+1).NodeId];
                            double MinusCost3 =  CostMatrix[rt.get(j).NodeId][rt.get(j+1).NodeId];

                            double AddedCost1 = CostMatrix[rt.get(i-1).NodeId][rt.get(i+1).NodeId];
                            double AddedCost2 = CostMatrix[rt.get(j).NodeId][rt.get(i).NodeId];
                            double AddedCost3 = CostMatrix[rt.get(i).NodeId][rt.get(j+1).NodeId];

                            NeigthboorCost = AddedCost1 + AddedCost2 + AddedCost3
                                    - MinusCost1 - MinusCost2 - MinusCost3;

                            if (NeigthboorCost < BestNCost) {
                                BestNCost = NeigthboorCost;
                                SwapIndexA  = i;
                                SwapIndexB  = j;
                                SwapRoute = VehIndex;

                            }
                        }
                    }
                }
            }

            if (BestNCost < 0) {

                rt = this.Vehicles[SwapRoute].Route;

                Node SwapNode = rt.get(SwapIndexA);

                rt.remove(SwapIndexA);

                if (SwapIndexA < SwapIndexB)
                { rt.add(SwapIndexB, SwapNode); }
                else
                { rt.add(SwapIndexB+1, SwapNode); }

                PastSolutions.add(this.Cost);
                this.Cost  += BestNCost;
            }
            else{
                Termination = true;
            }

            if (iteration_number == MAX_ITERATIONS)
            {
                Termination = true;
            }
        }
        PastSolutions.add(this.Cost);

        try{
            PrintWriter writer = new PrintWriter("PastSolutionsIntra.txt", "UTF-8");
            for  (int i = 0; i< PastSolutions.size(); i++){
                writer.println(PastSolutions.get(i)+"\t");
            }
            writer.close();
        } catch (Exception e) {}
    }

    public void SolutionPrint(String Solution_Label)//Print Solution In console
    {
        System.out.println("=========================================================");
        System.out.println(Solution_Label+"\n");

        for (int j=0 ; j < NoOfVehicles ; j++)
        {
            if (! Vehicles[j].Route.isEmpty())
            {   System.out.print("Vehicle " + j + ":");
                int RoutSize = Vehicles[j].Route.size();
                for (int k = 0; k < RoutSize ; k++) {
                    if (k == RoutSize-1)
                    { System.out.print(Vehicles[j].Route.get(k).NodeId );  }
                    else
                    { System.out.print(Vehicles[j].Route.get(k).NodeId+ "->"); }
                }
                System.out.println();
            }
        }
        System.out.println("\nSolution Cost "+this.Cost+"\n");
    }
}

class Node
{
    public int NodeId;
    public int Node_X ,Node_Y; //Node Coordinates
    public int demand; //Node Demand if Customer
    public boolean IsRouted;
    private boolean IsDepot; //True if it Depot Node

    public Node(int depot_x,int depot_y) //Cunstructor for depot
    {
        this.NodeId = 0;
        this.Node_X = depot_x;
        this.Node_Y = depot_y;
        this.IsDepot = true;
    }

    public Node(int id ,int x, int y, int demand) //Cunstructor for Customers
    {
        this.NodeId = id;
        this.Node_X = x;
        this.Node_Y = y;
        this.demand = demand;
        this.IsRouted = false;
        this.IsDepot = false;
    }
}

class Vehicle
{
    public int VehId;
    public ArrayList<Node> Route = new ArrayList<Node>();
    public int capacity;
    public int load;
    public int CurLoc;
    public boolean Closed;

    public Vehicle(int id, int cap)
    {
        this.VehId = id;
        this.capacity = cap;
        this.load = 0;
        this.CurLoc = 0; //In depot Initially
        this.Closed = false;
        this.Route.clear();
    }

    public void AddNode(Node Customer )//Add Customer to Vehicle Route
    {
        Route.add(Customer);
        this.load +=  Customer.demand;
        this.CurLoc = Customer.NodeId;
    }

    public boolean CheckIfFits(int dem) //Check if we have Capacity Violation
    {
        return ((load + dem <= capacity));
    }

}

class draw
{
    public static void  drawRoutes(Solution s, String fileName) {

        int VRP_Y = 800;
        int VRP_INFO = 200;
        int X_GAP = 600;
        int margin = 30;
        int marginNode = 1;


        int XXX = VRP_INFO + X_GAP;
        int YYY = VRP_Y;


        BufferedImage output = new BufferedImage(XXX, YYY, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, XXX, YYY);
        g.setColor(Color.BLACK);


        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (int k = 0; k < s.Vehicles.length ; k++)
        {
            for (int i = 0; i < s.Vehicles[k].Route.size(); i++)
            {
                Node n = s.Vehicles[k].Route.get(i);
                if (n.Node_X > maxX) maxX = n.Node_X;
                if (n.Node_X < minX) minX = n.Node_X;
                if (n.Node_Y > maxY) maxY = n.Node_Y;
                if (n.Node_Y < minY) minY = n.Node_Y;

            }
        }

        int mX = XXX - 2 * margin;
        int mY = VRP_Y - 2 * margin;

        int A, B;
        if ((maxX - minX) > (maxY - minY))
        {
            A = mX;
            B = (int)((double)(A) * (maxY - minY) / (maxX - minX));
            if (B > mY)
            {
                B = mY;
                A = (int)((double)(B) * (maxX - minX) / (maxY - minY));
            }
        }
        else
        {
            B = mY;
            A = (int)((double)(B) * (maxX - minX) / (maxY - minY));
            if (A > mX)
            {
                A = mX;
                B = (int)((double)(A) * (maxY - minY) / (maxX - minX));
            }
        }

        // Draw Route
        for (int i = 0; i < s.Vehicles.length ; i++)
        {
            for (int j = 1; j < s.Vehicles[i].Route.size() ; j++) {
                Node n;
                n = s.Vehicles[i].Route.get(j-1);

                int ii1 = (int) ((double) (A) * ((n.Node_X - minX) / (maxX - minX) - 0.5) + (double) mX / 2) + margin;
                int jj1 = (int) ((double) (B) * (0.5 - (n.Node_Y - minY) / (maxY - minY)) + (double) mY / 2) + margin;

                n = s.Vehicles[i].Route.get(j);
                int ii2 = (int) ((double) (A) * ((n.Node_X - minX) / (maxX - minX) - 0.5) + (double) mX / 2) + margin;
                int jj2 = (int) ((double) (B) * (0.5 - (n.Node_Y - minY) / (maxY - minY)) + (double) mY / 2) + margin;


                g.drawLine(ii1, jj1, ii2, jj2);
            }
        }

        for (int i = 0; i < s.Vehicles.length ; i++)
        {
            for (int j = 0; j < s.Vehicles[i].Route.size() ; j++) {

                Node n = s.Vehicles[i].Route.get(j);

                int ii = (int) ((double) (A) * ((n.Node_X  - minX) / (maxX - minX) - 0.5) + (double) mX / 2) + margin;
                int jj = (int) ((double) (B) * (0.5 - (n.Node_Y - minY) / (maxY - minY)) + (double) mY / 2) + margin;
                if (i != 0) {
                    g.fillOval(ii - 3 * marginNode, jj - 3 * marginNode, 6 * marginNode, 6 * marginNode); //2244
                    String id = Integer.toString(n.NodeId);
                    g.drawString(id, ii + 6 * marginNode, jj + 6 * marginNode); //88
                } else {
                    g.fillRect(ii - 3 * marginNode, jj - 3 * marginNode, 6 * marginNode, 6 * marginNode);  //4488
                    String id = Integer.toString(n.NodeId);
                    g.drawString(id, ii + 6 * marginNode, jj + 6 * marginNode); //88
                }
            }

        }

        String cst = "VRP solution for "+s.NoOfCustomers+ " customers with Cost: " + s.Cost;
        g.drawString(cst, 10, 10);

        fileName = fileName + ".png";
        File f = new File(fileName);
        try
        {
            ImageIO.write(output, "PNG", f);
        } catch (IOException ex) {
            //  Logger.getLogger(s.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}