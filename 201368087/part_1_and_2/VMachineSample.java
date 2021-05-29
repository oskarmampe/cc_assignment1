/*******************************************************************************
 * Coursework 1
 *
 * @author Oskar Mampe
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

/**
 * 
 * Class containing all the required methods for part 1 and part 2.
 * 
 */
public class VMachineSample{

    // Variables for client, and VM.
    private VirtualMachine vm;
    private Client oneClient;
    
    // Response from OpenNebula API.
    private OneResponse rc;
    private int newVMID;
    
    // Map needed for part 2 migration.
    private HashMap<Integer, Double> hostMap = new HashMap<>();

    // Template for VM.
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

    // Main method that runs both parts.
    public static void main(String[] args)
    {
        // Create the class
        VMachineSample sample = new VMachineSample();
        try {
            // Run part 1
            sample.part1();

            // Run part 2
            sample.part2();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * 
     * Part 1 VM creation and deletion.
     * 
     */
    private void part1() {
        try {
            // Connect to client using oneClient
            connectToClient();

            // Print the template that is going to be used.
            printTemplate();

            // Run the experiment 5 times.
            for (int i = 0; i < 5; i++) {
                System.out.println("------------ LOOP NUMBER " + i + " ------------");
                
                // Initialise the VM and print how long it took.
                initializeVM();

                // Print the VM info
                printVMInfo();

                // Delete the VM
                deleteVM();
            }
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 
     * Part 2 VM migration
     * 
    */
    private void part2() {
        try {
            // Connect to client using oneClient
            connectToClient();

            // Print the template that is going to be used.
            printTemplate();
            for (int i = 0; i < 5; i++) {
                System.out.println("------------ LOOP NUMBER " + i + " ------------");

                // Initialise the VM
                initializeVM();
                
                // Start timing.
                long startTime = System.currentTimeMillis();

                // Get the host info.
                getHostInfo();

                // Start migrating
                migrate();

                // Stop timing.
                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;

                // Print the time
                System.out.println("---------------------------------------------");
                System.out.println("Time Elapsed ... ");
                System.out.println(elapsed + " milliseconds");
                System.out.println("---------------------------------------------");

                // Print the VM info, to check whether the VM migrate was successful.
                printVMInfo();

            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

    private void printTemplate() {

        System.out.println("Virtual Machine Template:\n" + vmTemplate);
        System.out.println();
    }

    private void initializeVM() throws Exception {
        System.out.print("Trying to allocate the virtual machine... ");

        // Start Timing.
        long startTime = System.currentTimeMillis();

        // Allocate the resources for the VM
        rc = VirtualMachine.allocate(oneClient, vmTemplate);

        // If encountered an error from the API, quit out.
        if( rc.isError() )
        {
            System.out.println( "failed!");
            throw new Exception( rc.getErrorMessage() );
        }

        // The response message is the new VM's ID
        newVMID = Integer.parseInt(rc.getMessage());
        System.out.println("ok, ID " + newVMID + ".");

        // We can create a representation for the new VM, using the returned VM-ID
        vm = new VirtualMachine(newVMID, oneClient);

        // Deploy the machine
        rc = vm.deploy(10);

        // If encountered an error from the API, quit out.
        if(rc.isError())
        {
            System.out.println("failed!");
            throw new Exception( rc.getErrorMessage() );
        }
        else
            System.out.println("ok.");

        // And now we can request its information.
        rc = vm.info();

        // If encountered an error from the API, quit out.
        if(rc.isError())
            throw new Exception( rc.getErrorMessage() );

        // While the VM is not running, wait for it to change state to running
        while (!vm.status().equals("runn")) {
            Thread.sleep(10);// Sleep as not to send too many API requests.
            rc = vm.info();// Get the up-to-date info about the VM
        }

        // End timing
        long endTime = System.currentTimeMillis();

        long elapsed = endTime - startTime;

        // Print the time took
        System.out.println("---------------------------------------------");
        System.out.println("Time Elapsed ... ");
        System.out.println(elapsed + " milliseconds");
        System.out.println("---------------------------------------------");

        printVMInfo();

        // xpath expression to get the path to disk, to check whether the VM deployed successfully.
        System.out.println("The path of the disk is");
        System.out.println( "\t" + vm.xpath("template/disk/source") );
    }

    /**
     * 
     * Prints the VM information, as taken from the OpenNebula API.
     * 
     */
    private void printVMInfo() {
        // Get the info from the API
        rc = vm.info();

        // Print the info
        System.out.println();
        System.out.println(
                "This is the information OpenNebula stores for the VM:");
        System.out.println(rc.getMessage() + "\n");

        System.out.println("The new VM " +
                vm.getName() + " has status: " + vm.status());
    }

    /**
     * 
     * Connects to the API.
     * 
     * @throws Exception
     */
    private void connectToClient() throws Exception {

        // Credentials
        String passwd;
        String username = System.getProperty("user.name");

        // Ask the password for the user
        passwd = new String(System.console().readPassword("[%s]", "Password:"));

        // Connect to the client
        oneClient = new Client(username + ":" + passwd, "https://csgate1.leeds.ac.uk:2633/RPC2");
    }

    /**
     * 
     * Deleting a VM.
     * 
     */
    private void deleteVM() throws Exception{
        // Start timing.
        long startTime = System.currentTimeMillis();

        // Delete the VM with the current pointer.
        rc = vm.finalizeVM();

        // Wait for the VM to delete
        while (!vm.status().equals("done")) {
            Thread.sleep(10);
            OneResponse response = vm.info();
        }

        // End timing.
        long endTime = System.currentTimeMillis();

        long elapsed = endTime - startTime;

        // Print the elapsed time
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

    /**
     * 
     * Get the host info 
     * 
     */
    private void getHostInfo(){
        //                  CPU  MEM  DISK
        double[] weights = {0.5, 0.4, 0.1};

        // Usage of resources from the APU
        double cpuUsage, memUsage, diskUsage;
        int hostId;

        // OpenNebula pool containing all hosts.
		HostPool pool = new HostPool( oneClient );
		pool.info();

        // Printing a table
        System.out.println("Physical Hosts with resource usage:");
        System.out.println("-----------------------------------------------------------------");
        System.out.println(String.format("|%-15s|%-15s|%-15s|%-15s|", "HOSTID", "CPU USAGE", "MEM USAGE", "DISK USAGE"));
        
        // For each host
        for( Host host: pool)
		{
            // Get the inho of the host
            rc = host.info();
            
            // Get the resource usage.
			cpuUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/CPU_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_CPU")))*100;
			memUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MEM_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_MEM")))*100;
			diskUsage = (Double.parseDouble(host.xpath("/HOST/HOST_SHARE/DISK_USAGE"))/Double.parseDouble(host.xpath("/HOST/HOST_SHARE/MAX_DISK")))*100;
            hostId = Integer.parseInt(host.xpath("/HOST/ID"));
            
            // Calculate the heuristic
            double heuristic = Math.abs(cpuUsage) * weights[0] + Math.abs(memUsage) * weights[1] + Math.abs(diskUsage) * weights[2];
            
            // Put in map to get the minimum later
            hostMap.put(hostId, heuristic);

            //Print for table
            System.out.println(String.format("|%-15s|%-15s|%-15s|%-15s|", Integer.toString(hostId),
                    String.format("%.2f", cpuUsage), String.format("%.2f", memUsage), String.format("%.2f", diskUsage)));
		}
        System.out.println("-----------------------------------------------------------------");

    }

    /**
     * 
     * Migrate function, taking the minimum from map and migrating
     * 
     */
    private void migrate() throws Exception {

        // Get the minimum id
		Integer id = Collections.min(hostMap.entrySet(), Map.Entry.comparingByValue()).getKey();
		System.out.println(String.format("MIGRATING TO HOST %d AS IT HAS THE LOWEST HEURISTIC VALUE OF %.2f", id, hostMap.get(id)));

        // Migrate to the minimum host id
        rc = vm.migrate(id);

        // Get the VM info
        rc = vm.info();

        // Wait for the VM to migrate
        while (!vm.status().equals("runn")) {
            Thread.sleep(10);
            rc = vm.info();
        }
    }
}
