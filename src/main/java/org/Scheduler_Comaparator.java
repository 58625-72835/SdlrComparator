/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.aco.ACODatacenterBroker;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.hybrid.HybridDatacenterBroker;
import org.moga.MOGADatacenterBroker;
import org.utils.Constants;
import org.utils.DatacenterCreator;

/**
 *
 * @author Kunal Ranjan Patel
 */
public class Scheduler_Comaparator {
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static final Random randObj  = new Random(System.currentTimeMillis());
    private static Datacenter[] datacenter;
//    private static double mapping[];
//    private static double[][] commMatrix;
//    private static double[][] execMatrix;

    
    private static List<Vm> createVM(int userId, List<List<? extends  Number>> vmSpecsMatrix) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<Vm> list = new LinkedList<Vm>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name
        int vms = vmSpecsMatrix.get(0).size();
        

        for (int i = 0; i < vms; i++) {
            int ram =(int) vmSpecsMatrix.get(0).get(i); //vm memory (MB)
            int mips = (int) vmSpecsMatrix.get(1).get(i);
            long bw = (long) vmSpecsMatrix.get(2).get(i);
            System.out.println("Vm: "+Integer.parseInt(datacenter[i].getId()+""+i)+" Size: "+size+" Ram: "+ram+" MIPS: "+mips+" BW: "+bw);
            Vm vm = new Vm(Integer.parseInt(datacenter[i].getId()+""+i), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlet(int userId, List<List<? extends  Number>> clouldletSpecsMatrix, int idShift) {
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        int cloudlets = clouldletSpecsMatrix.get(0).size();
        

        for (int i = 0; i < cloudlets; i++) {
//            int dcId = (int) (mapping[i]);
//            long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
        long fileSize = (long) clouldletSpecsMatrix.get(0).get(i);
        long outputSize = (long) clouldletSpecsMatrix.get(1).get(i);
        long length = (long) clouldletSpecsMatrix.get(2).get(i);
        System.out.println("Cloudlet: "+(idShift+i)+" Length: "+length+" File Size: "+fileSize+" Output Size: "+outputSize);
        Cloudlet cloudlet = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(userId);
            list.add(cloudlet);
        }

        return list;
    }

    public static void main(String[] args) {
        for(int iter =0;iter<1;iter++){
            Log.printLine("For iteration :"+iter+" ...");
            Log.printLine("Starting  Scheduler Comparator...");

    //        new GenerateMatrices();
    //        commMatrix = GenerateMatrices.getCommMatrix();
    //        execMatrix = GenerateMatrices.getExecMatrix();
    //        PSOSchedularInstance = new PSO();
    //        mapping = PSOSchedularInstance.run();

            try {
                int num_user = 1;   // number of grid users
                boolean trace_flag = false;  // mean trace events

                List<Integer> vmRamSizes = new ArrayList<>();
                List<Integer> vmMipss= new ArrayList<>();
                List<Long> vmBws= new ArrayList<>();
                for(int i =0;i<Constants.NO_OF_DATA_CENTERS;i++){
                    vmRamSizes.add((512+randObj.nextInt(17)*32));
                    vmMipss.add((250+randObj.nextInt(5)*50));
                    vmBws.add((long)(1000+100*randObj.nextInt(10)));
                }
                List<List<? extends  Number>> vmSpecsMatrix = new ArrayList<>();
                vmSpecsMatrix.add(vmRamSizes);
                vmSpecsMatrix.add(vmMipss);
                vmSpecsMatrix.add(vmBws);


                List<Long> cloudletFileSizes = new ArrayList<>();
                List<Long> cloudletOutputSizes = new ArrayList<>();
                List<Long> cloudletLengths = new ArrayList<>();
                for(int i =0;i<Constants.NO_OF_TASKS;i++){
                    cloudletFileSizes.add((long)(300*(1.0+randObj.nextDouble())));
                    cloudletOutputSizes.add((long)(300*(1.0+randObj.nextDouble())));
                    cloudletLengths.add((long)(randObj.nextDouble()*600+500)*1000);
                }
                List<List<? extends  Number>> clouldletSpecsMatrix = new ArrayList<>();
                clouldletSpecsMatrix.add(cloudletFileSizes);
                clouldletSpecsMatrix.add(cloudletOutputSizes);
                clouldletSpecsMatrix.add(cloudletLengths);

                List<List<Number>> dcSpecsMatrix = new ArrayList<>();
                for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                    List<Number> specs = new ArrayList<>();
                    specs.add(1000);
                    specs.add(1);
                    specs.add(1);
                    specs.add(2048);
                    specs.add((1000000.0*(0.5+randObj.nextDouble())));
                    specs.add(10000);
                    dcSpecsMatrix.add(specs);
                }

                for(int index = 0;index<3;index++){
                    Calendar calendar = Calendar.getInstance();
                    CloudSim.init(num_user, calendar, trace_flag);
                    // Second step: Create Datacenters
                    datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
                    for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
                        datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i,dcSpecsMatrix.get(index));
                    }

                    //Third step: Create Broker
                    DatacenterBroker broker = createDatacenterBroker(index,"Broker_"+index);
                    //Third step: Create Broker
                    int brokerId = broker.getId();

                    //Fourth step: Create VMs and Cloudlets and send them to broker
                    vmList = createVM(brokerId, vmSpecsMatrix);
                    cloudletList = createCloudlet(brokerId, clouldletSpecsMatrix, index);

        //            // mapping our dcIds to cloudsim dcIds
        //            HashSet<Integer> dcIds = new HashSet<>();
        //            HashMap<Integer, Integer> hm = new HashMap<>();
        //            for (Datacenter dc : datacenter) {
        //                if (!dcIds.contains(dc.getId()))
        //                    dcIds.add(dc.getId());
        //            }
        //            Iterator<Integer> it = dcIds.iterator();
        //            for (int i = 0; i < mapping.length; i++) {
        //                if (hm.containsKey((int) mapping[i])) continue;
        //                hm.put((int) mapping[i], it.next());
        //            }
        //            for (int i = 0; i < mapping.length; i++)
        //                mapping[i] = hm.containsKey((int) mapping[i]) ? hm.get((int) mapping[i]) : mapping[i];

                    broker.submitVmList(vmList);
        //            broker.setMapping(mapping);
                    broker.submitCloudletList(cloudletList);


                    // Fifth step: Starts the simulation
                    CloudSim.startSimulation();

                    List<Cloudlet> newList = broker.getCloudletReceivedList();

                    CloudSim.stopSimulation();

                    printCloudletList(newList,broker);
    //                broker.
                }
                Log.printLine(Scheduler_Comaparator.class.getName() + " finished!");
            } catch (Exception e) {
                e.printStackTrace();
                Log.printLine("The simulation has been terminated due to an unexpected error");
            }
        }
    }
    private static DatacenterBroker createDatacenterBroker(int index,String name) throws Exception {
        switch (index) {
            case 0:
            return new MOGADatacenterBroker(name);                
            case 1:
            return new HybridDatacenterBroker(name);                
            case 2:
            return new ACODatacenterBroker(name);
            default:
                throw new AssertionError();
        }

    }
    
     

    /**
     * Prints the Cloudlet objects
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list,DatacenterBroker broker) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent +"length" + indent+ "STATUS" +
                indent + "Data center ID" +
                indent + "VM ID" +
                indent + indent + "Time" +
                indent + "Start Time" +
                indent + "Finish Time");

        double mxFinishTime = 0;
        DecimalFormat dft = new DecimalFormat("###.##");
        dft.setMinimumIntegerDigits(2);
        
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + dft.format(cloudlet.getCloudletId()) +indent + indent + dft.format(cloudlet.getCloudletLength())+ indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
                        indent + indent + indent + dft.format(cloudlet.getVmId()) +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) +
                        indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
            mxFinishTime = Math.max(mxFinishTime, cloudlet.getFinishTime());
        }
        Log.printLine(broker.getClass().getName()+": "+mxFinishTime);
//        PSOSchedularInstance.printBestFitness();
    }
}
