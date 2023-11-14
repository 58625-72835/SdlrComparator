/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ga;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.utils.Constants;
/**
 *
 * @author Kunal Ranjan Patel
 */



public class GADatacenterBroker extends DatacenterBroker{
    
    protected final Random randObj ;
    protected int crossoverFactor = 2;
    protected double mutatioonFactor = 0.5;
    public GADatacenterBroker(String name) throws Exception {
        super(name);
        randObj = new Random(System.currentTimeMillis());
        mutatioonFactor = 0.5;
        crossoverFactor = 2;
    }

    protected  List<Integer> createChromosome(int size){
        List<Integer> resChromosome = new ArrayList<>();
        for(int i=0; i<size;i++){
            resChromosome.add(vmList.get(randObj.nextInt(vmList.size())).getId());
        }
        return resChromosome;
    }
    
    protected List<Map<Integer,? extends Number >> getResourceUsedMaps(List<Integer> chromosome){
        List<Map<Integer,? extends Number>> maps = new ArrayList<>();
        Map<Integer,Integer> vmToDatacenterMap = getVmsToDatacentersMap();
        Map<Integer,Long> dcStorageUsedMap = new HashMap<>();
        Map<Integer,Long> dcMaxStorageUsedByCloudletMap = new HashMap<>();
        Map<Integer,Long> dcBwUsedMap = new HashMap<>();
        Map<Integer,Integer> dcMemoryUsedMap = new HashMap<>();
//        Map<Integer,Integer> dcPeUsedMap = new HashMap<>();

        Set<Integer> vmSet = new HashSet<>();

        for (int index=0;index<chromosome.size();index++){
            int vmId = chromosome.get(index);
            vmSet.add(vmId);
            Cloudlet cl = getCloudletList().get(index);
            int dcid = vmToDatacenterMap.get(vmId);
            dcMaxStorageUsedByCloudletMap.put(dcid,  Math.max(dcMaxStorageUsedByCloudletMap.getOrDefault(dcid,0l),cl.getCloudletFileSize()+cl.getCloudletOutputSize()));
        }
        for (int vmId:vmSet){
            Vm vm =     VmList.getById(vmList, vmId);
            int dcid = vmToDatacenterMap.get(vmId);
            dcStorageUsedMap.put(dcid,  dcStorageUsedMap.getOrDefault(dcid,0l)+vm.getSize());
            dcMemoryUsedMap.put(dcid,  dcMemoryUsedMap.getOrDefault(dcid,0)+vm.getRam());
//              dcPeUsedMap.put(dcid,  dcPeUsedMap.getOrDefault(dcid,0)+vm.getNumberOfPes());
            dcBwUsedMap.put(dcid,  dcBwUsedMap.getOrDefault(dcid,0l)+vm.getBw());
        }
        
        for (int key: dcStorageUsedMap.keySet()){
            dcStorageUsedMap.put(key, dcStorageUsedMap.get(key)+dcMaxStorageUsedByCloudletMap.getOrDefault(key,0l));
        }
        maps.add(dcStorageUsedMap);
        maps.add(dcMemoryUsedMap);
        maps.add(dcBwUsedMap);
//        maps.add(dcPeUsedMap);
        return maps;
    }
    
    protected double getResourceUtilization(List<Integer> chromosome){
        
        Map<Integer,DatacenterCharacteristics> datacenterCharacteristicsMap = getDatacenterCharacteristicsList();
        
        Map<Integer,Long> dcTotalBwMap = new HashMap<>();
        Map<Integer,Integer> dcTotalMemoryMap = new HashMap<>();        
//        Map<Integer,Integer> dcTotalPeMap = new HashMap<>();
        Map<Integer,Long> dcTotalStorageMap = new HashMap<>();
        
        for(int dcid:datacenterCharacteristicsMap.keySet()){
            
            for (Host h: datacenterCharacteristicsMap.get(dcid).getHostList()){
                dcTotalBwMap.put(dcid, dcTotalBwMap.getOrDefault(dcid,0l)+h.getBw());            
                dcTotalMemoryMap.put(dcid,  dcTotalMemoryMap.getOrDefault(dcid,0)+h.getRam());
//                dcTotalPeMap.put(dcid,  dcTotalPeMap.getOrDefault(dcid,0)+h.getNumberOfPes());
                dcTotalStorageMap.put(dcid,  dcTotalStorageMap.getOrDefault(dcid,0l)+h.getStorage());
            }

        }
        List<Map<Integer,? extends  Number>> resourceUsedMaps = getResourceUsedMaps(chromosome);
        Map<Integer,? extends Number> dcStorageUsedMap = resourceUsedMaps.get(0);
        Map<Integer,? extends Number> dcMemoryUsedMap = resourceUsedMaps.get(1);
        Map<Integer,? extends Number> dcBwUsedMap = resourceUsedMaps.get(2);
//        Map<Integer,? extends Number> dcPeUsedMap = resourceUsedMaps.get(3);

        
        List<Map<Integer,Double>> mapss = new ArrayList<>();
        mapss.add(new HashMap<>());
        mapss.add(new HashMap<>());
        mapss.add(new HashMap<>());        
//        mapss.add(new HashMap<>());        
        for(int key:dcStorageUsedMap.keySet()){
            mapss.get(0).put(key, Double.valueOf((long)dcStorageUsedMap.get(key))/Double.valueOf(dcTotalStorageMap.get(key)));
            mapss.get(1).put(key, Double.valueOf((int)dcMemoryUsedMap.get(key))/dcTotalMemoryMap.get(key));
            mapss.get(2).put(key, Double.valueOf((long)dcBwUsedMap.get(key))/dcTotalBwMap.get(key));
//            mapss.get(3).put(key, (1.0*dcPeUsedMap.get(key))/dcTotalPeMap.get(key));
        }
        boolean bv = false;
        double sumByCategory= 0.0;
        for(Map<Integer,Double> map:mapss){
            double sumByDaracenter = 0.0;
            for(int key:map.keySet()){
                if(map.get(key)>1){
                    bv = true;
                    return 0.0;
                }else{
                    sumByDaracenter+=map.get(key);
                }
            }
            sumByCategory+=(sumByDaracenter/map.keySet().size());
        }
        return bv?0.0:sumByCategory/mapss.size();
    }
    
    protected Map<Integer,Double> getVmtoTimeMap(List<Integer> chromosome){
        HashMap<Integer,Double> vmtoTimeMap = new HashMap<>();
        
        
        for (int index=0; index<chromosome.size();index++){
            int vmId = chromosome.get(index);
            Vm vm = VmList.getById(vmList, vmId);
            Cloudlet cl = getCloudletList().get(index);
            double time = (1.0*cl.getCloudletTotalLength())/(vm.getMips()*vm.getNumberOfPes());
            if(vmtoTimeMap.containsKey(vmId)){
                vmtoTimeMap.put(vmId,(time+ vmtoTimeMap.get(vmId)));
            }else{
                vmtoTimeMap.put(vmId, time);
            }
        }
        return vmtoTimeMap;
    }
    
    protected double getTotalTime(List<Integer> chromosome){
         Map<Integer,Double> vmtoTimeMap = getVmtoTimeMap(chromosome);
        double maxTime = 0.0;
        for(int key:vmtoTimeMap.keySet()) {
            maxTime = Math.max(maxTime, vmtoTimeMap.get(key));
         }
        return maxTime;
    }
    
    protected double getTotalCost(List<Integer> chromosome){
        double totalCoat =0.0;
        Map<Integer,Double> vmtoTimeMap = getVmtoTimeMap(chromosome);
        Map<Integer,Integer> vmToDatacenterMap = getVmsToDatacentersMap();
        List<Map<Integer,? extends  Number>> resourceUsedMaps = getResourceUsedMaps(chromosome);
        Map<Integer,? extends Number> dcStorageUsedMap = resourceUsedMaps.get(0);
        Map<Integer,? extends Number> dcMemoryUsedMap = resourceUsedMaps.get(1);
        Map<Integer,? extends Number> dcBwUsedMap = resourceUsedMaps.get(2);
//        Map<Integer,? extends Number> dcPeUsedMap = resourceUsedMaps.get(3);
        Map<Integer,DatacenterCharacteristics> datacenterCharacteristicsMap = getDatacenterCharacteristicsList();
        
        Map<Integer,Double> dctoTimeMap = new HashMap<>();
        for (int vmId:vmtoTimeMap.keySet()){
            int dcid =  vmToDatacenterMap.get(vmId);
            dctoTimeMap.put(dcid,(vmtoTimeMap.get(vmId)+dctoTimeMap.getOrDefault(dcid,0.0)));
        }
        
        for(int vmId: chromosome){
            int dcid =  vmToDatacenterMap.get(vmId);
            DatacenterCharacteristics dcCharacteristics = datacenterCharacteristicsMap.get(dcid);
            totalCoat += (dctoTimeMap.get(dcid)*dcCharacteristics.getCostPerSecond()+dcBwUsedMap.get(dcid).longValue()*dcCharacteristics.getCostPerBw()+dcMemoryUsedMap.get(dcid).longValue()*dcCharacteristics.getCostPerMem()+dcStorageUsedMap.get(dcid).longValue()*dcCharacteristics.getCostPerStorage());
        }

        return totalCoat;
    }
    
    
    protected double fitnessFunction(List<Integer> chromosome){
        return getResourceUtilization(chromosome);
    }
        
    protected List<List<Integer>> selectParent(List<List<Integer>> population){
        List<List<Integer>> selectedParents = new ArrayList<>();
        population = copy(population);
        population.sort((List<Integer> o1, List<Integer> o2) -> (int)(fitnessFunction(o1)-fitnessFunction(o2)));
        List<Double> weights = new ArrayList<>();
        
        double totalWeight = 0.0;
        
        for(List<Integer> chromosome: population){
            totalWeight += fitnessFunction(chromosome);
            weights.add(totalWeight);
        }
        
        double selectedweight1 =  randObj.nextDouble()*totalWeight;
        double selectedweight2 =  randObj.nextDouble()*totalWeight;

        int firstIndex = -1;
        for (int index =0;index<population.size();index++){
            if(weights.get(index)>=selectedweight1){
                selectedParents.add(population.get(index));
                firstIndex = index;
                break;
            }else{
//                System.out.println("1 "+selectedweight1);
            }
        }    
        for (int index =0;index<population.size();index++){
            if(weights.get(index)>=selectedweight2){
                while (firstIndex == index){
                    index=randObj.nextInt(weights.size());
                }
                selectedParents.add(population.get(index));
                break;
            }else{
//                System.out.println("2 "+selectedweight1);
            }
        }
        
        while(selectedParents.size()<2){
            selectedParents.add(population.get(randObj.nextInt(population.size())));
        }
        
        return selectedParents;        
    }
    
    protected <T> List<T> copy(List<T> list){
        List<T> copy = new  ArrayList<>();
        for (T t: list){
            copy.add(t);
        }
        return copy;
    }
    
    protected <T> List<T> randomchoices(List<T> list, int n){
        List<Integer> selectedIndices = new ArrayList<>();
        for (int index=0;index<list.size();index++){
            selectedIndices.add(index);
        }
        for (int i=n;i<list.size();i++){
            selectedIndices.remove(randObj.nextInt(selectedIndices.size()));
        }
        
        List<T> selectedList = new ArrayList<>();
        for(int index:selectedIndices){
            selectedList.add(list.get(index));
        }
        return selectedList;
    }
    
    protected List<List<Integer>>  crossOver(List<Integer> chromosome1,List<Integer> chromosome2, int crossoverFactor){
        
        chromosome1 = copy(chromosome1);
        chromosome2 = copy(chromosome2);

        int halfOfSize = chromosome1.size()/2;
        int numberOfIdices =crossoverFactor;
        List<Integer> selectedIndices = new ArrayList<>();
        for (int index=0;index<chromosome1.size();index++){
            selectedIndices.add(index);
        }
        
        selectedIndices = randomchoices(selectedIndices, numberOfIdices);

        for(int index:selectedIndices){
            int t = chromosome1.get(index);
            chromosome1.set(index, chromosome2.get(index));
            chromosome2.set(index, t);
        }
        List<List<Integer>> offsprings = new ArrayList<>();        
        offsprings.add(chromosome1);
        offsprings.add(chromosome2);
        
        return  offsprings;

    }
        
    
    protected List<Integer> mutate(List<Integer> chromosome,double mutationFactor){
        chromosome = copy(chromosome);
        List<Integer> availableVmIds  = new ArrayList<>();
        
        for(Vm vm : vmList){
            availableVmIds.add(vm.getId());
        }
                
        int numberOfIdices = Math.max(1,randObj.nextInt(chromosome.size()/4));
        List<Integer> selectedIndices = new ArrayList<>();
        for (int index=0;index<chromosome.size();index++){
            selectedIndices.add(index);
        }
        selectedIndices = randomchoices(selectedIndices, numberOfIdices);
        
        for(int index : selectedIndices ){
            if (randObj.nextDouble()<=mutationFactor) {
                chromosome.set(index, availableVmIds.get(randObj.nextInt(availableVmIds.size())));
            }
        }
        

        return chromosome;
    }
    
    protected void updatePopulation(List<List<Integer>> population, List<Integer> chromosome){
        int minFitnessChromosomeIndex = 0;
        for(int index=0;index<population.size();index++){
            if (fitnessFunction(population.get(index))<fitnessFunction(population.get(minFitnessChromosomeIndex))){
                minFitnessChromosomeIndex = index;
            }
        }
        if(fitnessFunction(chromosome)>fitnessFunction(population.get(minFitnessChromosomeIndex))){
            population.set(minFitnessChromosomeIndex, chromosome);
        }
    }
    
    protected double averageFitnessOf(List<List<Integer>> population){
        double sum = 0.0;
        for(List<Integer> chromosome:population){
            sum += fitnessFunction(chromosome);
        }
        return sum/population.size();
    }
    
    protected List<List<Integer>> initPopulation(int size,int chromosomeSize){
        
        List<List<Integer>> resPopulation = new ArrayList<>();
        
        for (int i=0;i<3*size;i++){
            List<Integer> newChromosome = (createChromosome(chromosomeSize));
//            System.out.println(newChromosome.toString()+"");
            if(!resPopulation.contains(newChromosome)&&fitnessFunction(newChromosome)>=(averageFitnessOf(resPopulation)/2)){
                resPopulation.add(newChromosome);
            }
        }
        while(resPopulation.size()<size){
            List<Integer> newChromosome = (createChromosome(chromosomeSize));
            if (!resPopulation.contains(newChromosome)) {
                resPopulation.add(newChromosome);
            }
        }
        return resPopulation;
    }
    
    private List<Integer> geneticAlgorithm(List<List<Integer>> population,int maxGen){
//        System.out.println(""+population.toString());
        List<Integer> prevBestChromosome = null;
        double prevBestFitness = 0;
        int numberGen = 0;
        for (int gen =0;gen<maxGen;gen++){
            for(int iter =0;iter<population.size()/2;iter++){
                List<List<Integer>> selectedParents = selectParent(population);
//                System.out.println("Parents"+selectedParents.toString());
                List<List<Integer>> offspring = crossOver(selectedParents.get(0),selectedParents.get(1),crossoverFactor);
                updatePopulation(population, offspring.get(0));
                updatePopulation(population, offspring.get(1));
                List<Integer> mutatedChromosome1 = mutate(offspring.get(0),mutatioonFactor);
                updatePopulation(population, mutatedChromosome1);
                List<Integer> mutatedChromosome2 = mutate(offspring.get(1),mutatioonFactor);
                updatePopulation(population, mutatedChromosome2);
            }
            List<Integer> bestChromosome = population.get(0);
            for(List<Integer> chromosome : population){
                if (fitnessFunction(chromosome)>fitnessFunction(bestChromosome)){
                    bestChromosome = chromosome;
                }
            }
            System.out.println(gen+" : "+getResourceUtilization(bestChromosome)+" : "+getTotalCost(bestChromosome)+" : "+getTotalTime(bestChromosome)+" : "+fitnessFunction(bestChromosome)+" : "+(bestChromosome.toString()));
            numberGen ++;
            System.out.println(""+numberGen);
            if(prevBestChromosome == null){
                prevBestChromosome = bestChromosome;
                prevBestFitness = fitnessFunction(bestChromosome);
            }else if(prevBestFitness >= fitnessFunction(bestChromosome)){
                if (prevBestFitness == fitnessFunction(bestChromosome)&&!prevBestChromosome.equals(bestChromosome)) {
                    prevBestChromosome = bestChromosome;
                    numberGen = 0;
                }else if(numberGen ==3&&gen>=maxGen/4){
                    mutatioonFactor = (mutatioonFactor ==1.0)?1.0:((mutatioonFactor+1)/2);
                    crossoverFactor = Math.min(Math.max(2,2*crossoverFactor), (int) (0.75*population.size()));
                } else if(numberGen >=maxGen/8&&gen>=maxGen/4){
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
        }
        List<Integer> bestChromosome = population.get(0);
        for(List<Integer> chromosome : population){
            if (fitnessFunction(chromosome)>fitnessFunction(bestChromosome)){
                bestChromosome = chromosome;
            }
        }
        Log.printLine(getClass().getName()+": Solution: "+bestChromosome.toString());
        Log.printLine("Fitness: "+fitnessFunction(bestChromosome));
        Log.printLine("Resource Utilization: "+getResourceUtilization(bestChromosome));
        Log.printLine("Total Time: "+getTotalTime(bestChromosome));
        Log.printLine("Total Cost: "+getTotalCost(bestChromosome));
        return bestChromosome;
    }

    //scheduling function
    protected Integer[] createMapping() {

        ArrayList<Cloudlet> clist = new ArrayList<>();

        for (Cloudlet cloudlet : getCloudletList()) {
            clist.add(cloudlet);
        }
        
        
        List<List<Integer>> population = initPopulation(Constants.POPULATION_SIZE,clist.size());
        
        List<Integer> selectedChromosome = geneticAlgorithm(population,Constants.MAX_ITERATION);
        
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
    private List<Cloudlet> assignCloudletsToVms(List<Cloudlet> cloudlist) {
        int idx = 0;
        Integer[] mapping = createMapping();
        for (Cloudlet cl : cloudlist) {
            cl.setVmId(mapping[idx++]);
        }
        return cloudlist;
    }

    @Override
    protected void submitCloudlets() {
        List<Cloudlet> tasks = assignCloudletsToVms(getCloudletList());
        int vmIndex = 0;
        for (Cloudlet cloudlet : tasks) {
            Vm vm;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // submit to the specific vm
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { // vm was not created
                    Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
                            + cloudlet.getCloudletId() + ": bount VM not available");
                    continue;
                }
            }

            Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                    + cloudlet.getCloudletId() + " to VM #" + vm.getId());
            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
        }
    }

    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            distributeRequestsForNewVmsAcrossDatacenters();
        }
    }

    protected void distributeRequestsForNewVmsAcrossDatacenters() {
        int numberOfVmsAllocated = 0;
        int i = 0;

        final List<Integer> availableDatacenters = getDatacenterIdsList();

        for (Vm vm : getVmList()) {
            int datacenterId = availableDatacenters.get(i++ % availableDatacenters.size());
            String datacenterName = CloudSim.getEntityName(datacenterId);

            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                numberOfVmsAllocated++;
            }
        }

        setVmsRequested(numberOfVmsAllocated);
        setVmsAcks(0);
    }
}
