package bayes_network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bayes_network.cpd.CPDQuery;

import pair.Pair;

import data.attribute.Attribute;

/**
 * Objects of this class encapsulate a complete Bayesian Network.
 * 
 * @author Matthew Bernstein - matthewb@cs.wisc.edu
 *
 */
public class BayesianNetwork 
{
    /**
     * Sets verbose output on or off
     */
    public boolean verbose = true;

    /**
     * Network structure inference algorithms
     */
    public static enum Type { TEST, NAIVE_BAYES, TAN, HILL_CLIMBING,
                               SPARSE_CANDIDATE };
  

    /**
     * The algorithm used to build the network
     */
    private Type netInference;

    /**
     * A mapping of attributes to their corresponding node in the network.
     */
    Map<Attribute, BNNode> nodes;

    /**
     * Constructor
     */
    public BayesianNetwork()
    {
        this.nodes = new HashMap<Attribute, BNNode>();
    }

    public void setNetInference(BayesianNetwork.Type netInference)
    {
        this.netInference = netInference;
    }

    /**
     * @return an ArrayList holding all Node objects in the network
     */
    public ArrayList<BNNode> getNodes()
    {
        Collection<BNNode> col = nodes.values();
        return new ArrayList<BNNode>(col);
    }

    /**
     * Add a new Node to the network
     * 
     * @param newNode the new Node
     */
    public void addNode(BNNode newNode)
    {
        this.nodes.put(newNode.getAttribute(), newNode);
    }

    /**
     * Retrieve a Node according to the Attribute this Node represents
     * 
     * @param attr the Attribute represented by the Node of interest
     * @return the Node that represents this Attribute
     */
    public BNNode getNode(Attribute attr)
    {
        return this.nodes.get(attr);
    }

    /**
     * Create a directed edge in the network
     * 
     * @param parent the parent Node of the edge
     * @param child the child Node of the edge
     */
    public void createEdge(BNNode parent, BNNode child)
    {
        parent.addChild(child);
        child.addParent(parent);
    }

    /**
     * Remove a directed edge from the network
     * 
     * @param parent the parent Node of the edge
     * @param child the child Node of the edge
     */
    public void removeConnection(BNNode parent, BNNode child)
    {
        parent.getChildren().remove(child);
        child.getParents().remove(parent);
    }

    @Override
    public String toString()
    {

        String result = "\n\n";

        // For each node, print its parents
        for (BNNode node : nodes.values())
        {	
            result += node.getAttribute().getName();

            for (BNNode parent : node.getParents())
            {
                result += " ";
                result += parent.getAttribute().getName();
            }
            result += "\n";

            if (verbose)
            {
                result += "\n\n";
                result += node.getCPD().toString();
                result += "\n";
            }
        }

        return result;
    }

    /**
     * Query for a conditional probability in the Bayes net.  This method
     * returns the probability for the value of a specific attribute in the 
     * network conditioned on a set of values for other variables in the
     * network.  For example, this method is used for calculated probabilities
     * of the form P(A = a | E = e, D = d).
     *   
     * @param query
     * @return
     */
    public Double queryConditionalProbability(BNConditionalQuery query)
    {        
        /*
         * The conditional probability P(A = a | E = e, D = d) is found by
         * calculating:
         * 
         *  P(B = b, E = e, D = d) / P(E = e, D = d).  
         */
        
        
        /*
         * Calculate the numerator.
         */
        BNJointQuery allVarJointQuery 
                      = new BNJointQuery( query.getAllVariableSet() );
                                           
        Double numerator = queryJointProbability(allVarJointQuery);
        
        /*
         * Calculate the denominator.
         */
        BNJointQuery conditionVarJointQuery 
                       = new BNJointQuery( query.getConditionalVariableSet() );
        Double denominator = queryJointProbability(conditionVarJointQuery);
        
        return numerator / denominator;
    }
    
    //TODO: CALCULATE THE JOINT PROBABILITY
    public Double queryJointProbability(BNJointQuery query)
    {
        /*
         * Contains all nodes for which we need to make a query into their
         * CPD table
         */
        Set<BNNode> allNodes = new HashSet<BNNode>();
        
        /*
         * Retrieve all nodes we need to consider in the calculation
         */
        for (Pair<Attribute, Integer> variable : query.getVariables())
        {
            BNNode queryNode = this.getNode(variable.getFirst());
            
            /*
             *  Get a list of all nodes that precede each variable node in the 
             *  network's DAG structure
             */
            allNodes.addAll( getNodesAbove(queryNode)  );          
        }
        
        /*
         * Parition all of the nodes under consideration into a set of nodes
         * for which we need to iterate over all values and nodes for which
         * the values are specified in the query.
         */
        Pair<ArrayList<BNNode>, ArrayList<BNNode>> partition
                = separateSpecifiedUnspecified(allNodes, query);
       
        ArrayList<BNNode> unspecified = partition.getSecond();
       
        /*
         * Run enumeration to get the joint probability
         */
        Double jointProbability = runEnumeration(query.getVariables(), unspecified);
        
        System.out.println("Joint Probability: " + jointProbability);
        
        return jointProbability;
    }
    
    /**
     * A recursive algorithm that runs the inference by enumeration to get
     * the joint probability of a query.
     * <br>
     * <br>
     * For example, if we have a network with the following adjacency-list:
     * <br>
     * <br>
     * B -> A
     * C -> A
     * C -> D
     * <br>
     * <br>
     * And we wish to query P(A = a, D = d), the enumeration we must perform
     * is:
     * <br>
     * &Sigma; b &isin; B &Sigma; c &isin; C P(A = a | B = b) * P(B = b) * 
     * P(A = a | C = c) * P(C = c) * P(D = d | C = d) 
     * 
     * @param currValues a list of attribute/value pairs that have already 
     * been assigned in the current enumeration
     * @param toIterate a list of nodes for which we still need to enumerate
     * over their values
     * @return return the joint probability result of the enumeration
     */
    @SuppressWarnings("unchecked")
    private Double runEnumeration(ArrayList<Pair<Attribute, Integer>> currValues, 
                                  ArrayList<BNNode> toIterate)
    {
        if (toIterate.size() == 0)  // Stopping condition
        {
            /*
             * Calculate a single term in the enumeration
             */
            return calculateProbability(currValues);
        }
        else
        {
            Double sum = 0.0;
            
            /*
             * Get the current attribute we are iterating over
             */
            Attribute attr = toIterate.get(0).getAttribute();
            toIterate.remove(0);
                        
            for (Integer nominalValue : attr.getNominalValueMap().values())
            {      
                /*
                 * Current attribute/value pair of the current attribute 
                 */
                Pair<Attribute, Integer> newPair 
                        = new Pair<Attribute, Integer>(attr, nominalValue);
                
                currValues.add( newPair );
                
                /*
                 * Recursive call
                 */
                sum += runEnumeration(currValues, (ArrayList<BNNode>) toIterate.clone());
                
                currValues.remove( newPair );
            }
            
            return sum;
        }
        
    }
    
    /**
     * Calculate a single term in the enumeration
     * 
     * @param values the attribute/value pairs in the term of the enumeration
     * @return the product of each term in the product
     */
    private Double calculateProbability(ArrayList<Pair<Attribute, Integer>> values)
    {  
        // TODO: DOCUMENT!    
        
        Double product = 1.0;
                
        for (Pair<Attribute, Integer> pair : values)
        {
            
            BNNode node = this.getNode(pair.getFirst());
            
            CPDQuery cpdQuery = buildCPDQuery(node, values);
            
            System.out.print(" = " + node.query(cpdQuery) + " * " + "\n");
            
            product *= node.query(cpdQuery);
        }
        
        System.out.println("\n");
               
        return product;
    }
    
    /**
     * 
     * @param node
     * @param queryDetails
     * @return
     */
    private CPDQuery buildCPDQuery(BNNode node, 
                                   ArrayList<Pair<Attribute, Integer>> queryDetails )
    { 
        // TODO: DOCUMENT THIS!
        
        String cpdStr = "P(" + node.getAttribute().getName() + " = ";
        
        CPDQuery query = new CPDQuery();
        
        for (Pair<Attribute, Integer> q : queryDetails)
        {
            if (q.getFirst().equals(node.getAttribute()))
            {
                query.addQueryItem(q.getFirst(), q.getSecond());
                
                cpdStr += q.getFirst().getNominalValueName(q.getSecond()) + " | ";
            }
        }
        
        for (BNNode parent : node.getParents())
        {
            for (Pair<Attribute, Integer> q : queryDetails)
            {
                if (q.getFirst().equals(parent.getAttribute()))
                {
                    query.addQueryItem(q.getFirst(), q.getSecond());
                    
                    cpdStr += q.getFirst().getName() + " = ";
                    cpdStr += q.getFirst().getNominalValueName(q.getSecond()) + " ";
                }
            }
        }
        
        cpdStr += ")";
        
        System.out.print(cpdStr);
        
        return query;
    }
    
    /**
     * Gets all nodes above a certain node in the network DAG structure
     * including the node itself.
     * <br>
     * <br>
     * Example: if we have a network with the following adjacency-list:
     * <br>
     * <br>
     * A -> B <br>
     * D -> A <br>
     * E -> A <br>
     * <br>
     * <br>
     * This method would return B,A,E,D for the node B.<br>
     * This method would return A,E,D for the node A. <br> 
     * 
     * @param node the query node
     * @return all nodes above the query node in the DAG structure of the net
     */
    public Set<BNNode> getNodesAbove(BNNode node)
    {
        Set<BNNode> aboveNode = new HashSet<BNNode>();
        aboveNode.add(node);
        
        if (node.getParents().size() == 0)  // Stopping Criteria
        {
            return aboveNode;
        }
        else                        
        {
            for (BNNode parent : node.getParents())
            {
                /*
                 *  Recursive call to each parent of the query node 
                 */
                aboveNode.addAll( getNodesAbove(parent) );
            }
            return aboveNode;
        }
    }

    /**
     * @return the name of the algorithm that was used to infer this Bayes 
     * network's structure
     */
    private String getNetInferenceName()
    {
        String result = "";

        switch(netInference)
        {
        case NAIVE_BAYES:
            result = "Naive Bayes";
            break;
        case TAN:
            result = "TAN";
            break;
        case TEST:
            result = "Test";
            break;
        case HILL_CLIMBING: 
            result = "Hill Climbing";
            break;
        case SPARSE_CANDIDATE:
            result = "Sparse Candidate";
            break;
        }

        return result;
    }

    /**
     * @param netInference the algorithm used to create the network's
     * structure
     */
    protected void setInference(BayesianNetwork.Type netInference)
    {
        this.netInference = netInference;
    }

    /**
     * A helper method for separating a set of nodes into two partitions based
     * on a joint probability query.  Say we have a query (A = a, B = b), both
     * of these nodes may form the total set of nodes (A, B, C, D, E) that must
     * be considered in the probability calculation.  This method separates 
     * (A, B, C, D, E) -> (A, B) + (C, D, E) the nodes specified in the query
     * and the nodes not specified in the query but must be considered.
     * 
     * @param nodes all nodes considered in the probability calculation
     * @param query the query for the probability
     * @return the partition of all nodes to be considered into a list of nodes
     * specified in the query (first element) and those that are not specified
     * (second element).
     */
    private Pair<ArrayList<BNNode>, ArrayList<BNNode>> 
    separateSpecifiedUnspecified(Collection<BNNode> nodes, BNJointQuery query)
    {

        /*
         * Create array lists for holding each partition
         */
        ArrayList<BNNode> specified = new ArrayList<BNNode>();
        ArrayList<BNNode> unspecified = new ArrayList<BNNode>(nodes);
        
        /*
         * Separate the collection into the two partions
         */
        for (Pair<Attribute, Integer> variable : query.getVariables())
        {
            BNNode specifiedNode = this.getNode(variable.getFirst());
            
            int index = unspecified.indexOf( specifiedNode );
            specified.add( unspecified.get(index) );
            unspecified.remove(index);
        }
        
        /*
         * Build result pair
         */
        Pair<ArrayList<BNNode>, ArrayList<BNNode>> result 
                    = new Pair<ArrayList<BNNode>, ArrayList<BNNode>>();
        result.setFirst(specified);
        result.setSecond(unspecified);
        
        return result;
    }
    
}
