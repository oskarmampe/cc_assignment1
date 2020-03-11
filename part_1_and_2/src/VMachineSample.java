/*******************************************************************************
 * VM Example class - Coursework 1
 *
 * @author Karim Djemame
 * @version 1.0 [2020-02-21]
 *
 *******************************************************************************/


import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.opennebula.client.host.Host;
import org.opennebula.client.host.HostPool;

import java.util.*;

public class VMachineSample{

    private VirtualMachine vm;
    private Client oneClient;
    private OneResponse rc;
    private int newVMID;
    private HashMap<Integer, Double> hostMap = new HashMap<>();

    // We will try to create a new virtual machine. The first thing we
    // need is an OpenNebula virtual machine template.

    // This VM template is a valid one, but it will probably fail to run
    // if we try to deploy it; the path for the image is unlikely to
    // exist.
    private String vmTemplate =
            "CPU=\"0.1\"\n"
            + "SCHED_DS_REQUIREMENTS=\"ID=101\"\n"
            + "NIC=[\n"
            + "\tNETWORK_UNAME=\"oneadmin\",\n"
            + "\tNETWORK=\"vnet1\" ]\n"
            + "LOGO=\"images/logos/linux.png\"\n"
            + "DESCRIPTION=\"A ttylinux instance with VNC and network context scripts, available for testing purposes. In raw format.\"\n"
            + "DISK=[\n"
            + "\tIMAGE_UNAME=\"oneadmin\",\n"
            + "\tIMAGE=\"ttylinux Base\" ]\n"
            + "SUNSTONE_NETWORK_SELECT=\"YES\"\n"
            + "SUNSTONE_CAPACITY_SELECT=\"YES\"\n"
            + "MEMORY=\"128\"\n"
            + "HYPERVISOR=\"kvm\"\n"
            + "GRAPHICS=[\n"
            + "\tLISTEN=\"0.0.0.0\",\n"
            + "\tTYPE=\"VNC\" ]\n";

    public static void main(String[] args)
    {
        VMachineSample sample = new VMachineSample();
        try {
            sample.part1();
            sample.part2();
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }

    private void part1() {
        try {
            connectToClient();
            printTemplate();
            for (int i = 0; i < 5; i++) {
                System.out.println("------------ LOOP NUMBER " + i + " ------------");
                initializeVM();
                printVMInfo();
                deleteVM();
            }
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void part2() {
        try {
            connectToClient();
            printTemplate();
            for (int i = 0; i < 5; i++) {

                System.out.println("------------ LOOP NUMBER " + i + " ------------");

                initializeVM();

                long startTime = System.currentTimeMillis();

                getHostInfo();
                migrate();

                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;
                System.out.println("---------------------------------------------");
                System.out.println("Time Elapsed ... ");
                System.out.println(elapsed + " milliseconds");
                System.out.println("---------------------------------------------");

                printVMInfo();

            }
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void printPools (VirtualMachinePool vmPool, HostPool hostPool)
    {
        System.out.println("--------------------------------------------");
        System.out.println("Number of VMs: " + vmPool.getLength());
        System.out.println("User ID\t\tName\t\tEnabled");

        // You can use the for-each loops with the OpenNebula pools
        for( VirtualMachine vm : vmPool )
        {
            String id = vm.getId();
            String name = vm.getName();
            String enab = vm.xpath("enabled");

            System.out.println(id+"\t\t"+name+"\t\t"+enab);
        }

        System.out.println("--------------------------------------------");
        System.out.println("Number of Hosts: " + hostPool.getLength());
        System.out.println("Host ID\t\tName\t\tEnabled");


        // You can use the for-each loops with the OpenNebula pools
        for( Host host : hostPool )
        {
            String id = host.getId();
            String name = host.getName();
            String enab = host.xpath("enabled");

            System.out.println(id+"\t\t"+name+"\t\t"+enab);
        }



        System.out.println("--------------------------------------------");
    }

    private void printTemplate() {

        System.out.println("Virtual Machine Template:\n" + vmTemplate);
        System.out.println();
    }

    private void initializeVM() throws Exception {
        System.out.print("Trying to allocate the virtual machine... ");
        long startTime = System.currentTimeMillis();

        rc = VirtualMachine.allocate(oneClient, vmTemplate);

        if( rc.isError() )
        {
            System.out.println( "failed!");
            throw new Exception( rc.getErrorMessage() );
        }

        // The response message is the new VM's ID
        newVMID = Integer.parseInt(rc.getMessage());
        System.out.println("ok, ID " + newVMID + ".");

        // We can create a representation for the new VM, using the returned
        // VM-ID
        vm = new VirtualMachine(newVMID, oneClient);

        rc = vm.deploy(10);
        // while vm.state() not running

        if(rc.isError())
        {
            System.out.println("failed!");
            throw new Exception( rc.getErrorMessage() );
        }
        else
            System.out.println("ok.");

        // And now we can request its information.
        rc = vm.info();

        if(rc.isError())
            throw new Exception( rc.getErrorMessage() );

        while (!vm.status().equals("runn")) {
            Thread.sleep(10);
            rc = vm.info();
        }

        long endTime = System.currentTimeMillis();

        long elapsed = endTime - startTime;

        System.out.println("---------------------------------------------");
        System.out.println("Time Elapsed ... ");
        System.out.println(elapsed + " milliseconds");
        System.out.println("---------------------------------------------");

        printVMInfo();

        // And we can also use xpath expressions
        System.out.println("The path of the disk is");
        System.out.println( "\t" + vm.xpath("template/disk/source") );
    }

    private void printVMPool() throws Exception {
        // Let's delete the VirtualMachine object.
        vm = null;

        // The reference is lost, but we can ask OpenNebula about the VM
        // again. This time however, we are going to use the VM pool
        VirtualMachinePool vmPool = new VirtualMachinePool(oneClient);
        HostPool hostPool = new HostPool(oneClient);
        // Remember that we have to ask the pool to retrieve the information
        // from OpenNebula
        rc = vmPool.info();

        if(rc.isError())
            throw new Exception( rc.getErrorMessage() );

        rc = hostPool.info();

        if(rc.isError())
            throw new Exception( rc.getErrorMessage() );

        System.out.println(
                "\nThese are all the Virtual Machines in the pool:");

        printPools(vmPool, hostPool);
        for ( VirtualMachine vmachine : vmPool )
        {
            System.out.println("\tID :" + vmachine.getId() +
                    ", Name :" + vmachine.getName() );

            // Check if we have found the VM we are looking for
            if ( vmachine.getId().equals( ""+newVMID ) )
            {
                vm = vmachine;
            }
        }

    }

    private void printVMInfo() {
        rc = vm.info();

        System.out.println();
        System.out.println(
                "This is the information OpenNebula stores for the VM:");
        System.out.println(rc.getMessage() + "\n");

        System.out.println("The new VM " +
                vm.getName() + " has status: " + vm.status());
    }

    private void connectToClient() throws Exception {

        String passwd;

        String username = System.getProperty("user.name");
        passwd = new String(System.console().readPassword("[%s]", "Password:"));

        oneClient = new Client(username + ":" + passwd, "https://csgate1.leeds.ac.uk:2633/RPC2");
    }

    private void deleteVM() {
        long startTime = System.currentTimeMillis();

        rc = vm.finalizeVM();

        long endTime = System.currentTimeMillis();

        long elapsed = endTime - startTime;

        System.out.println("---------------------------------------------");
        System.out.println("Time Elapsed ... ");
        System.out.println(elapsed + " milliseconds");
        System.out.println("---------------------------------------------");

        System.out.println("\nTrying to finalize (delete) the VM " +
                vm.getId() + "...");

        System.out.println("\tOpenNebula response");
        System.out.println("\t Error: " + rc.isError());
        System.out.println("\t Msg: " + rc.getMessage());
        System.out.println("\t ErrMsg: " + rc.getErrorMessage());
    }

    private void getHostInfo(){
        //                  CPU  MEM  DISK
        double[] weights = {0.5, 0.4, 0.1};

        double cpuUsage, memUsage, diskUsage;
        int hostId;

		HostPool pool = new HostPool( oneClient );
		pool.info();

        System.out.println("Physical Hosts with resource usage:");
        System.out.println("-----------------------------------------------------------------");
        System.out.println(String.format("|%-15s|%-15s|%-15s|%-15s|", "HOSTID", "CPU USAGE", "MEM USAGE", "DISK USAGE"));
		for( Host host: pool)
		{
			rc = host.info();
			cpuUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/CPU_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_CPU")))*100;
			memUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MEM_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_MEM")))*100;
			diskUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/DISK_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_DISK")))*100;
			hostId = Integer.parseInt(host.xpath("/HOST/ID"));
			double heuristic = Math.abs(cpuUsage) * weights[0] + Math.abs(memUsage) * weights[1] + Math.abs(diskUsage) * weights[2];
			hostMap.put(hostId, heuristic);
            System.out.println(String.format("|%-15s|%-15s|%-15s|%-15s|", Integer.toString(hostId),
                    String.format("%.2f", cpuUsage), String.format("%.2f", memUsage), String.format("%.2f", diskUsage)));
		}
        System.out.println("-----------------------------------------------------------------");

    }

    private void migrate() throws Exception {

		Integer id = Collections.min(hostMap.entrySet(), Map.Entry.comparingByValue()).getKey();
		System.out.println(String.format("MIGRATING TO HOST %d AS IT HAS THE LOWEST HEURISTIC VALUE OF %.2f", id, hostMap.get(id)));

        rc = vm.migrate(id);

        rc = vm.info();

        while (!vm.status().equals("runn")) {
            Thread.sleep(10);
            rc = vm.info();
        }
    }
}
