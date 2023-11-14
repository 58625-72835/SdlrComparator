/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.moga.MOGADatacenterBroker;
import org.utils.Constants;

/**
 *
 * @author Kunal Ranjan Patel
 */
public class ACODatacenterBroker extends MOGADatacenterBroker{
    private List<Map<Integer, Double>>pheromoneMatrix, heuristicMatrix;
    private double evaporationRate = 0.5;
    private static double alpha = 1.0;
    private static double beta = 2.0;
    private  List<Integer> vmIdList;
    public ACODatacenterBroker(String name) throws Exception {
        super(name);
        
    }
    private int numCloudlets, numVms;
    private void initPheromones(){
        if (vmIdList == null){
            vmIdList = new ArrayList<>();
            for (Vm vm:vmList){
                vmIdList.add(vm.getId());
            }
        }
        pheromoneMatrix = new ArrayList<>();
        for (int i=0;i<numCloudlets;i++){
            Map <Integer,Double> m = new HashMap<>();
            for(int j=0;j<numVms;j++){
                m.put(vmIdList.get(j), randObj.nextDouble()*5);
            }
            pheromoneMatrix.add(m);
        }
        
    }
    private void initHeuristic(List<List<Integer>> population){
        if (vmIdList == null){
            vmIdList = new ArrayList<>();
            for (Vm vm:vmList){
                vmIdList.add(vm.getId());
            }
        }
        heuristicMatrix = new ArrayList<>();
        List<Integer> bestFitSolution = population.get(0);
        for(int i=1;i<numCloudlets;i++){
            if (bestFitSolution == null || fitnessFunction(bestFitSolution)<fitnessFunction(population.get(i))){
                bestFitSolution = population.get(i);
            }
        }
         for(int i=0;i<numCloudlets;i++){
            Map <Integer,Double> m = new HashMap<>();
            for (int j=0;j<numVms;j++){
                m.put(vmIdList.get(j),(double)(bestFitSolution.get(j)-population.get(randObj.nextInt(population.size())).get(j))  );
            }
            heuristicMatrix.add(m);
        }
        
    }
    protected double ACOEvaluateSolution(List<Integer> solution){
        return fitnessFunction(solution);
    }
    private void updatePheromoneLevels(List<Integer>solution, double solutionQuality,double bestSolnQuality, double w) {
    for (int task = 0; task < numVms; task++) {
        int resource = solution.get(task);
        pheromoneMatrix.get(task).put(resource,  pheromoneMatrix.get(task).get(resource)+(bestSolnQuality - solutionQuality)*w*randObj.nextDouble());
    }
    }
    
    private void evaporatePheromone() {
        for (int task = 0; task < numCloudlets; task++) {
            for (int resource:pheromoneMatrix.get(task).keySet()) {
                pheromoneMatrix.get(task).put(resource,  pheromoneMatrix.get(task).get(resource)* (1.0 - evaporationRate));
            }
        }
    }  
    private List<Integer> generateSolution() {
        List<Integer> solution = new ArrayList<>();
        for (int task = 0; task < numCloudlets; task++) {
            int selectedVm = selectVmForTask(task);
            solution.add(selectedVm);
        }

        return solution;
    }   

    private int selectVmForTask(int task) {
        double[] probabilities = new double[numVms];
        double totalProbability = 0.0;
        int i =0;
        for (int vmId   :vmIdList) {
            // Calculate the probability of selecting each resource for the task
            double pheromoneFactor = Math.pow(pheromoneMatrix.get(task).get(vmId), alpha);
            double heuristicFactor = Math.pow(heuristicMatrix.get(task).get(vmId), beta);
            probabilities[i] = pheromoneFactor * heuristicFactor;
            totalProbability += probabilities[i++];
        }

        // Choose a resource based on the calculated probabilities
        double randomValue = randObj.nextDouble() * totalProbability;
        double cumulativeProbability = 0.0;

        for (int vm = 0; vm < numVms; vm++) {
            cumulativeProbability += probabilities[vm];
            if (cumulativeProbability >= randomValue) {
                return vmList.get(vm).getId();
            }
        }

    // If the loop finishes without returning a resource, return the last resource as a fallback
    return vmList.get(numVms-1).getId();
}

    protected List<List<Integer>> ACOIteration(int numAnts,double w){
        List<List<Integer>> antColony = new ArrayList<>();
        double bestSolnQuality = Double.MIN_VALUE;
         for (int ant = 0; ant < numAnts; ant++) {
            List<Integer> solution = generateSolution();
            double solutionQuality = ACOEvaluateSolution(solution);
            if (bestSolnQuality<solutionQuality){
                bestSolnQuality = solutionQuality;
            }
            antColony.add(solution);
            updatePheromoneLevels(solution, solutionQuality,bestSolnQuality,w);
        }

        evaporatePheromone();
        
        return antColony;
    }
     
    private List<Integer> ACOAlgorithm(List<List<Integer>> population,int maxGen){
        numCloudlets =  population.get(0).size();
        numVms =vmList.size();
        initHeuristic(population);
        initPheromones();
        List<Integer> prevBestChromosome = null;
        double prevBestFitness = 0;
        int numberGen = 0;
        double wmax = 10+randObj.nextDouble() *20;        
        double wmin = randObj.nextDouble() *5;
        

        for (int gen =0;gen<maxGen;gen++){
             
            double w = wmax -(wmax-wmin)*(gen/maxGen);
            List<List<Integer>> acoResult = ACOIteration(population.size(),w);
//            for(List<Integer> solution: acoResult){
//                updatePopulation(population, solution);
//            }
            population = acoResult;
            List<Integer> bestChromosome = population.get(0);
            for(List<Integer> chromosome : population){
                if (fitnessFunction(chromosome)>fitnessFunction(bestChromosome)){
                    bestChromosome = chromosome;
                }
            }
            numberGen ++;
//            System.out.println(""+numberGen);
            if(prevBestChromosome == null){
                prevBestChromosome = bestChromosome;
                prevBestFitness = fitnessFunction(bestChromosome);
            }else if(prevBestFitness >= fitnessFunction(bestChromosome)){
                if (prevBestFitness == fitnessFunction(bestChromosome)&&!prevBestChromosome.equals(bestChromosome)) {
                    prevBestChromosome = bestChromosome;
                    numberGen = 0;
                }else  if(numberGen ==maxGen/8&&gen>=maxGen/4){
                    System.out.println(""+numberGen);
                    break;
                }                
            }else {
                if(!prevBestChromosome.equals(bestChromosome)){
                    prevBestChromosome = bestChromosome;
                    numberGen = 0;
                }
                if(numberGen ==maxGen/8&&gen>=maxGen/4){
                    System.out.println(""+numberGen);
                    break;
                }    
            }
            double bestFitness = fitnessFunction(bestChromosome);
            double fitnessDifference = averageFitnessOf(acoResult)-bestFitness;
            for (int i=0;i<weights.length;i++){
                weights[i] -= (0.01*(fitnessDifference/bestFitness));
            }
            System.out.println(gen+" "+getResourceUtilization(bestChromosome)+" "+getTotalCost(bestChromosome)+" "+getTotalTime(bestChromosome)+" "+fitnessFunction(bestChromosome));
            
        }
        List<Integer> bestChromosome = population.get(0);
        for(List<Integer> chromosome : population){
            if (fitnessFunction(chromosome)>fitnessFunction(bestChromosome)){
                bestChromosome = chromosome;
            }
        }
        bestChromosome = prevBestChromosome;
        Log.printLine(getClass().getName()+": Solution: "+bestChromosome.toString());
        Log.printLine("Fitness: "+fitnessFunction(bestChromosome));
        Log.printLine("Resource Utilization: "+getResourceUtilization(bestChromosome));
        Log.printLine("Total Time: "+getTotalTime(bestChromosome));
        Log.printLine("Total Cost: "+getTotalCost(bestChromosome));
        
        return bestChromosome;
    }

    //scheduling function
    @Override
    protected Integer[] createMapping() {

        ArrayList<Cloudlet> clist = new ArrayList<>();

        for (Cloudlet cloudlet : getCloudletList()) {
            clist.add(cloudlet);
        }
        
        
        weights = new double[3];
        for (int i=0;i<weights.length;i++){
            weights[i] = 1.0/3;
        }
        List<List<Integer>> population = initPopulation(Constants.MAX_ITERATION,clist.size());
        
        List<Integer> selectedChromosome = ACOAlgorithm(population,Constants.MAX_ITERATION);
        
        clist.get(0);
        

        setCloudletReceivedList(clist);
        Integer result[] = new Integer[selectedChromosome.size()];
        for (int i =0; i<selectedChromosome.size();i++){
            result[i] = selectedChromosome.get(i);
        }
        return result;
    }
}
