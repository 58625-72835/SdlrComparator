/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hybrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
public class HybridDatacenterBroker extends MOGADatacenterBroker {
    
    private List<Map<Integer, Double>>pheromoneMatrix, heuristicMatrix;
    private double evaporationRate = 0.5;
    private static double alpha = 1.0;
    private static double beta = 2.0;
    private  List<Integer> vmIdList;
    private Map<List<Integer>,Integer> tabuList;
    public HybridDatacenterBroker(String name) throws Exception {
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
                m.put(vmIdList.get(j),  (double)(bestFitSolution.get(j)-population.get(randObj.nextInt(population.size())).get(j)));
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
        List<List<Integer>> bestResults = new ArrayList<>();
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
            
        while(bestResults.size()<5){
            List<Integer> bestSolution = antColony .get(0);
            for(List<Integer> sol:antColony){
                if(ACOEvaluateSolution(bestSolution)<ACOEvaluateSolution(sol)){
                    bestSolution = sol;
                }
            }
            bestResults.add(bestSolution);
            antColony.remove(bestSolution);
        }
        
        return bestResults;
    }
    
    
    protected List<Integer> tabuIteration(List<Integer> solution){
        if(tabuList==null){
            tabuList = new HashMap<>();
        }
        List<Integer> bestNeighbor = null;
        
        for( int i=0;i<solution.size();i++){
            List<Integer> bestNeighborForIndex = null;
            for(int vm:vmIdList){
                if(vm!=solution.get(i)){
                    List<Integer> newNeighbor = copy(solution);
                    newNeighbor.set(i, vm);
                    if(bestNeighborForIndex == null || fitnessFunction(newNeighbor)>fitnessFunction(bestNeighborForIndex)){
                        bestNeighborForIndex = newNeighbor;
                    }
                }
            }
            if(bestNeighborForIndex !=null) {
                tabuList.put(bestNeighborForIndex, 6);
                if(bestNeighbor == null ||fitnessFunction(bestNeighborForIndex)>fitnessFunction(bestNeighbor)){
                    bestNeighbor = bestNeighborForIndex;
                }
            }

        }
        
        
//        for(List<Integer> key:tabuList.keySet()){
//            tabuList.put(key, tabuList.get(key)-1);
//            if(tabuList.get(key) <= 0){
//                tabuList.remove(key);
//            }
//        }

    Iterator<List<Integer>> iterator = tabuList.keySet().iterator();
    while (iterator.hasNext()) {
        List<Integer> key = iterator.next();
        tabuList.put(key, tabuList.get(key) - 1);
        if (tabuList.get(key) <= 0) {
            iterator.remove(); // Safely remove the entry
        }
    }
        return  bestNeighbor != null && fitnessFunction(bestNeighbor)>fitnessFunction(solution)?bestNeighbor:solution;
    }
    
    private List<Integer> hybridAlgorithm(List<List<Integer>> population,int maxGen){
        numCloudlets =  population.get(0).size();
        numVms =vmList.size();
        initHeuristic(population);
        initPheromones();
        List<List<Integer>> poretoSet = findNonDominateSolutions(population);
        poretoSet.sort((List<Integer> o1, List<Integer> o2) -> (int)(fitnessFunction(o1)-fitnessFunction(o2)));
        numElites = Math.min((3*poretoSet.size()/4),population.size()/5);
        List<Integer> prevBestChromosome = null;
        double prevBestFitness = 0;
        int numberGen = 0;
        double wmax = 10+randObj.nextDouble() *20;        
        double wmin = randObj.nextDouble() *5;
        

        for (int gen =0;gen<maxGen;gen++){
            for(int iter =0;iter<population.size()/2;iter++){
                List<List<Integer>> selectedParents = selectParent(population);
//                System.out.println("Parents"+selectedParents.toString());
                List<List<Integer>> offspring = crossOver(selectedParents.get(0),selectedParents.get(1),this.crossoverFactor);
                updatePopulation(population, offspring.get(0));
                updatePopulation(population, offspring.get(1));
                List<Integer> mutatedChromosome1 = mutate(offspring.get(0),mutatioonFactor);
                updatePopulation(population, mutatedChromosome1);
                List<Integer> mutatedChromosome2 = mutate(offspring.get(1),mutatioonFactor);
                updatePopulation(population, mutatedChromosome2);
            }
            List<Integer> selectedIndices = new ArrayList<>();
            for(int i =0;i<population.size();i++){
                selectedIndices.add(i);
            }
            while(selectedIndices.size()>numElites){
                selectedIndices.remove(randObj.nextInt(selectedIndices.size()));
            }
            for(int i=0;i<numElites;i++){
                population.set(selectedIndices.get(i), poretoSet.get(i));
            }
            
            double w = wmax -(wmax-wmin)*(gen/maxGen);
            List<List<Integer>> acoResult = ACOIteration(population.size(),w);
            for(List<Integer> solution: acoResult){
                updatePopulation(population, solution);
            }
            List<Integer> bestChromosome = population.get(0);
            for(List<Integer> chromosome : population){
                if (fitnessFunction(chromosome)>fitnessFunction(bestChromosome)){
                    bestChromosome = chromosome;
                }
            }
            bestChromosome = tabuIteration(bestChromosome);
//            System.out.println(gen+" : "+getResourceUtilization(bestChromosome)+" : "+getTotalCost(bestChromosome)+" : "+getTotalTime(bestChromosome)+" : "+fitnessFunction(bestChromosome)+" : "+(bestChromosome.toString()));
            numberGen ++;
//            System.out.println(""+numberGen);
            if(prevBestChromosome == null){
                prevBestChromosome = bestChromosome;
                updatePopulation(poretoSet, bestChromosome);
                prevBestFitness = fitnessFunction(bestChromosome);
            }else if(prevBestFitness >= fitnessFunction(bestChromosome)){
                if (prevBestFitness == fitnessFunction(bestChromosome)&&!prevBestChromosome.equals(bestChromosome)) {
                    prevBestChromosome = bestChromosome;
                updatePopulation(poretoSet, bestChromosome);
                    numberGen = 0;
                }else if(numberGen ==3&&gen>=maxGen/4){
                    mutatioonFactor = (mutatioonFactor ==1.0)?1.0:((mutatioonFactor+1)/2);
                    crossoverFactor = Math.min(Math.max(2,2*crossoverFactor), (int) (0.75*population.size()));
                } else if(numberGen ==maxGen/8&&gen>=maxGen/4){
                    System.out.println(""+numberGen);
                    break;
                }                
            }else { 
                 if(!prevBestChromosome.equals(bestChromosome)){
                    prevBestChromosome = bestChromosome;
                    numberGen = 0;
                }
                if(numberGen ==1&&gen>=maxGen/4){
                    mutatioonFactor = (mutatioonFactor ==1.0)?0.5:(2*mutatioonFactor-1);
                    crossoverFactor = Math.min(Math.max(2,crossoverFactor/2), (int) (0.75*population.size()));
                }else if(numberGen ==3&&gen>=maxGen/4){
                    mutatioonFactor = (mutatioonFactor ==1.0)?1.0:((mutatioonFactor+1)/2);
                    crossoverFactor = Math.min(Math.max(2,2*crossoverFactor), (int) (0.75*population.size()));
                } else if(numberGen ==maxGen/8&&gen>=maxGen/4){
                    System.out.println(""+numberGen);
                    break;
                }
                System.out.println(gen+" "+getResourceUtilization(bestChromosome)+" "+getTotalCost(bestChromosome)+" "+getTotalTime(bestChromosome)+" "+fitnessFunction(bestChromosome));

            }
            double bestFitness = fitnessFunction(bestChromosome);
            double fitnessDifference = averageFitnessOf(poretoSet)-bestFitness;
            for (int i=0;i<weights.length;i++){
                weights[i] -= (0.01*(fitnessDifference/bestFitness));
            }
            
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
        
        List<Integer> selectedChromosome = hybridAlgorithm(population,Constants.MAX_ITERATION);
        
        clist.get(0);
        

        setCloudletReceivedList(clist);
        Integer result[] = new Integer[selectedChromosome.size()];
        for (int i =0; i<selectedChromosome.size();i++){
            result[i] = selectedChromosome.get(i);
        }
        return result;
    }
}
