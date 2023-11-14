/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.moga;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.ga.GADatacenterBroker;
import org.utils.Constants;

/**
 *
 * @author Kunal Ranjan Patel
 */
public class MOGADatacenterBroker extends GADatacenterBroker{

    protected double[] weights;
    protected int numElites;
    public MOGADatacenterBroker(String name) throws Exception {
        super(name);
        weights = null;
    }

    
    protected boolean dominates(List<Integer> first,List<Integer> second) {
        double f11,f12,f21,f22,f31,f32;
        f11 = getResourceUtilization(first);
        f12 = getResourceUtilization(second);
        f21 = getTotalCost(first);
        f22 = getTotalCost(second);
        f31 = getTotalTime(first);
        f32 = getTotalTime(first);
        return (f11 >= f12 && f21 <= f22 && f31 <= f32)   && (f11 > f12 || f21 < f22 || f31 < f32); 
    }


protected List<List<Integer>> findNonDominateSolutions(List<List<Integer>> population) {
  List<List<Integer>> nonDominatedSolutionss = new ArrayList<List<Integer>>(); 
  for (List<Integer> solution : population) { 
    boolean isDominated = false; 
    for (List<Integer> other : population) { 
      if (dominates(solution,other)) {
          isDominated = true; 
        break;       }
    }
    if (!isDominated) { 
      nonDominatedSolutionss.add(solution); 
    }
  }
  return nonDominatedSolutionss; 
}
    

    @Override
    protected double fitnessFunction(List<Integer> chromosome){
        double resourceUtilization = getResourceUtilization(chromosome);
        if(resourceUtilization ==0.0){
            return -Double.MAX_VALUE;
        }
        return (weights[0]*1e9*resourceUtilization-weights[1]*getTotalCost(chromosome)-weights[2]*1e2*getTotalTime(chromosome));
    }
        
   
    private List<Integer> MOGAlgorithm(List<List<Integer>> population,int maxGen){
        List<List<Integer>> poretoSet = findNonDominateSolutions(population);
        poretoSet.sort((List<Integer> o1, List<Integer> o2) -> (int)(fitnessFunction(o1)-fitnessFunction(o2)));
        numElites = Math.min((3*poretoSet.size()/4),population.size()/5);
        List<Integer> prevBestChromosome = null;
        double prevBestFitness = 0;
        int numberGen = 0;
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
            }
            double bestFitness = fitnessFunction(bestChromosome);
            double fitnessDifference = averageFitnessOf(poretoSet)-bestFitness;
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
        List<List<Integer>> population = initPopulation(Constants.POPULATION_SIZE,clist.size());
        
        List<Integer> selectedChromosome = MOGAlgorithm(population,Constants.MAX_ITERATION);
        
        clist.get(0);
        

        setCloudletReceivedList(clist);
        Integer result[] = new Integer[selectedChromosome.size()];
        for (int i =0; i<selectedChromosome.size();i++){
            result[i] = selectedChromosome.get(i);
        }
        return result;
    }

//    @Override
//    protected void processCloudletReturn(SimEvent ev) {
//        Cloudlet cloudlet = (Cloudlet) ev.getData();
//        getCloudletReceivedList().add(cloudlet);
//        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
//                + " received");
//        cloudletsSubmitted--;
//        if (getCloudletList().isEmpty() && cloudletsSubmitted == 0) {
//            createMapping();
//            cloudletExecution(cloudlet);
//        }
//    }
//
//
//    protected void cloudletExecution(Cloudlet cloudlet) {
//
//        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
//            Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
//            clearDatacenters();
//            finishExecution();
//        } else { // some cloudlets haven't finished yet
//            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
//                // all the cloudlets sent finished. It means that some bount
//                // cloudlet is waiting its VM be created
//                clearDatacenters();
//                createVmsInDatacenter(0);
//            }
//
//        }
//    }
//    
   
}
