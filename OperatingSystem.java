import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

// The OS class that includes the kernel/driver, loader, schedulers, device objects, etc.
class OperatingSystem {

    // Object for instantiating classes for the Memory Manager, IO Controller, Process PCB, and CPU
    // These objects represent the corresponding components as the chips/devices/structures that the kernel controls through the OSML layer.
    // They include the necessary variables and methods, as you will see later in the code.
    MemoryManagementUnit MMU;
    IOController IOC;
    ProcessControlBlock PCB;
    CentralProcessingUnit CPU;
    
    // These are two queues managed by the long-term and short-term schedulers.
    // They are used to schedulea queue of jobs and then shift them into the CPU when ready.
    // These queues were implemented as PCB arrays to better represent them as lists of processes.
    // The PCB objects in these arrays represent the jobs (processes) and give the schedulers access to the process states.
    ProcessControlBlock[] jobQueue;
    ProcessControlBlock[] readyQueue;

    // The OS Driver (Kernel) method.
    // This method takes in the path of the program file that needs to be executed and employs the loader, schedulers, and CPU to execute said program.
    // The code for this method was kept as simple as possible to represent the entire logic and flow of program execution as concisely as possible.
    // Thus, this method mostly instantiates objects and calls methods using those objects.
    // It is a high-level encapsulation of the entire VM2024 architecture.
    void driver(String programPath) {
        // Instantiates two objects for two devices: the IO Controller and Memory Management Unit.
        // These represent the IO/memory devices that will be used later.
        IOC = new IOController();
        MMU = new MemoryManagementUnit();

        // Creates two empty job and ready queues to house the jobs that will be executed.
        // Although only job is being run in our example, the arrays were initialized at 10 to simulate multiple jobs.
        jobQueue = new ProcessControlBlock[10];
        readyQueue = new ProcessControlBlock[10];

        // The driver inputs the program path into the loader.
        loader(programPath);

        // Then the driver calls for the long-term scheduler.
        longTermScheduler();
        
        // Before scheduling the task, the driver uses the newly created devices and process PCB to connect to and start the CPU.
        // The CPU gets access to the necessary PCB, memory, and is loaded with the correct memory offset for addressing the program data.
        CPU = new CentralProcessingUnit(MMU, PCB);

        // As all components are ready, the short-term scheduler is called to find and shift the process into the ready queue.
        shortTermScheduler();

        // Now that the process is ready, PCB, and PC are set, the driver iterates through all of the instructions of the program, executing the data path cycle through the CPU for each instruction.
        while(PCB.programCounter < PCB.instructionCount) {
            // Syncs the PCB and CPU program counters.
            CPU.PC = PCB.programCounter;
            // Runs the data path cycle.
            CPU.run();
            // Iterates the program counter.
            PCB.programCounter++;
        }
    }

    // The loader method, responsible for opening the program file and saving the contents into the virtual disc.
    // This method works off of a virtual input port that resembles an IO connection between the virtual disc and the abritrary location of the program file.
    // The method opens the port, scans through the document, and adds the hexcode line-by-line to the virtual disc. 
    void loader(String path) {

        File programFile = new File(path);
        Scanner inputPort = new Scanner(programFile);

        while (inputPort.hasNextLine()) {
            IOC.write(inputPort.nextLine());
        }

        inputPort.close();
    }

    // The long-term scheduler method, responsible for preparing the program to be dispatched as a job.
    // Transfer the program from virtual disc to a page in virtual memory, creates a PCB for the program with variables for state, and pushes the program to the job queue.
    void longTermScheduler() {

        // Iterates through the Virtual Disc and adds the program line-by-line to a page in virtual memory.
        for (int i = 0; i < IOC.getUsedSpace(); i++) {
            MMU.write(IOC.read(i));
        }

        // Deletes the first line containing "job" for readability and simplicity.
        if (MMU.read(0).toLowerCase().contains("job")) {
            MMU.delete(0);
        }

        // Initializes a new PCB for the program.
        PCB = new ProcessControlBlock();

        // Iterates through the page looking for break between instructions/data.
        // Then computes the instruction count, data offset in memory, saves them in PCB, and creates a break for data section.
        for (int i = 0; i < MMU.getUsedSpace(); i++) {
            if (MMU.read(i).toLowerCase().contains("data")) {
                PCB.dataMemoryOffset = i;
                PCB.instructionCount = i;
                MMU.override(i,"data");
            }
        }

        // Adds a line to the end of the page to signify end of the program.
        MMU.write("end");

        // Sets the PCB's variables to some initial values, prepares it for dispatching.
        PCB.pageNumber = 1;
        PCB.programCounter = 0;
        PCB.ready = true;

        // Pushes the program into the job queue.
        jobQueue[0] = PCB;
    }

    // The short-term scheduler method, for finding and dispatching the program for execution.
    void shortTermScheduler() {
        // Iterates through the job queue until it finds a ready program.
        int i = 0;
        while(!jobQueue[i].ready) {
            i++;
        }
        // Then dispatches that program.
        dispatch(0);
    }

    // Dispatcher method, to be used by the short-term scheduler.
    void dispatch(int i) {
        // Adds the program to ready queue and sets the CPU's PC for execution.
        readyQueue[i] = jobQueue[i];
        CPU.PC = readyQueue[i].programCounter;
    }
}

// A class for creating Process Control Block objects, containing relevant state variables for the program.
class ProcessControlBlock {
    public int programCounter;
    
    // The offset between instructions and data in memory, used for data addressing in the CPU.
    public int dataMemoryOffset;
    
    // Number of page used in memory.
    public int pageNumber;
    
    public int instructionCount;
    
    // Boolean to represent whether the program is ready for execution or not.
    public boolean ready;
}

// A a class for creating a IO Controller object that manages the virtual disc and handles IO operations.
class IOController {

    // Array List of Strings, representing our virtual disc and IO device.
    private ArrayList<String> virtualDisc;
    
    // Constructor that initializes this device.
    IOController() { 
        virtualDisc = new ArrayList<String>();
    }

    // Read IO Operation that returns a specfic line from disc.
    public String read(int i) {
        return virtualDisc.get(i);
    }

    // Write IO Operation that writes to latest line in disc.
    public void write(String s) {
        virtualDisc.add(s);
    }

    // IO Operation that returns the number of used lines in disc.
    public int getUsedSpace() {
        return virtualDisc.size();
    }
}

// A class for creating a Memory Management Unit object that manages virtual memory and handles memory operations.
class MemoryManagementUnit {

    // Array List of String to represent virtual memory.
    private ArrayList<String> virtualMemory;

    // Constructor that initializes this device.
    MemoryManagementUnit() {
        virtualMemory = new ArrayList<String>();
    }
    
    // Read Operation that returns a specfic line from memory.
    public String read(int i) {
        return virtualMemory.get(i);
    }

    // Write Operation that writes to the latest line in memory.
    public void write(String s) {
        virtualMemory.add(s);
    }
    
    // Write Operation that writes to a specific line in memory.
    public void write(int i, String s) {
        virtualMemory.add(i, s);
    }

    // Write Operation that writes over a specific line in memory.
    public void override(int i, String s) {
        virtualMemory.set(i, s);
    }

    // Delete Operation that removes a line from memory.
    public void delete(int i) {
        virtualMemory.remove(i);
    }

    // Memory Operation that returns the number of used lines in memory.
    public int getUsedSpace() {
        return virtualMemory.size();
    }
}

// High class to represent the CPU, its features, and its data path cycle.
class CentralProcessingUnit {
    // Set of CPU registers.
    public int[] registers;
    // CPU Variable for managing the index offset for data in the virtual memory; used to handle effective addressing.
    public int dataMemoryOffset; 
    // CPU Register for storing the Program Counter
    public int PC;
    // Access to Memory Manager and PCB.
    public MemoryManagementUnit memory;
    public ProcessControlBlock process;

    // Constructor for initializing the CPU as a device, requires a PCB for the process and MMU for memory access. 
    CentralProcessingUnit(MemoryManagementUnit mmu, ProcessControlBlock pcb) {

        // Instantiates the variables for memory, PCB, and offset.
        memory = mmu;
        process = pcb;
        dataMemoryOffset = pcb.dataMemoryOffset;

        // Initializes the registers, with each one being set to 0.
        registers = new int[16];
    }

    // Method used to carry out the fetch part of the data path cycle.
    void fetch() {
        // Adds the instruction from the PC to the instruction register for decoding.
        registers[2] = Integer.parseInt(memory.read(PC));
    }

    // Method used to decode the instruction from the above method and pass into execution.
    int[] decode() {

        // Gets the instruction from the instruction register, converting to binary as needed.
        String instruction = Integer.toBinaryString(registers[2]);

        // Finds the instruction type and opcode from the binary string.
        int type = Integer.parseInt(instruction.substring(0,2));
        int opcode = Integer.parseInt(instruction.substring(2,8));

        // Creates the operations and operands from the instruction type, creating an stream of data that can be put into the ALU for execution.
        // Note that the decode step handles this operation specific to the instruction type and manages the different registers and address lengths.
        
        // For Arithmetic Instructions, returns opcode, 3 registers, and the address.
        if (type == 0) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int reg3 = Integer.parseInt(instruction.substring(16,20));
            int address = Integer.parseInt(instruction.substring(20,24));
            int[] operation = {opcode,reg1,reg2,reg3,address};
            return operation;
        }

        // For Conditional Branch / Immediate Format Instructions, returns opcode, 2 registers, and the address.
        if (type == 1) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int address = Integer.parseInt(instruction.substring(16,32));
            int[] operation = {opcode,reg1,reg2,address};
            return operation;
        }

        // For Unconditional Jump Instructions, returns opcode and the address.
        if (type == 2) {    
            int address = Integer.parseInt(instruction.substring(8,32));
            int[] operation = {opcode,address};
            return operation;
        }

        // For IO Instructions, returns opcode, 2 registers, and the address.
        if (type == 3) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int address = Integer.parseInt(instruction.substring(16,32));
            int[] operation = {opcode,reg1,reg2,address};
            return operation;
        }

        // Returns only the opcode if there is no valid type.
        int[] operation = {opcode};
        return operation;
    }

    // Method used to represent the ALU executing instructions for the data path cycle.
    // A large switch statement that executes a function based on the opcode from decode().
    void execute(int[] operation) {
        // Gets stream of operation and operands from the decode step.
        // Then uses a switch statement to represent all possible opcodes and carries out operation from the opcodes using the proper operands.
        switch(operation[0]) {


            // Uses a case for each opcode and carries out the operation with the operands using the registers and memory access as needed, also checks for the address being null to see if the register or address needs to be used for specific operations.
            // Cases represent opcodes in numerical order as from the assignment table.
            
            // RD - Reads data at address or register.
            case 0:
            if (operation[3]==0) {
                registers[operation[1]] = registers[operation[2]];
            } else {
                registers[operation[1]] = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            }
            break;
            
            // WR - Writes data to address or register.
            case 1:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memory.override(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            // ST - Stores data to address or register.
            case 2:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memory.override(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            // LW - Loads data from register or address into other register.
            case 3:
            if (operation[3]==0) {
                registers[operation[1]] = registers[operation[2]];
            } else {
                registers[operation[1]] = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            }
            break;

            // MOV - Transfers data from one register to another.
            case 4:
            registers[operation[1]] = registers[operation[2]];
            break;

            // ADD - Adds two registers into other the register.
            case 5:
            registers[operation[1]] = registers[operation[2]] + registers[operation[3]];
            break;

            // SUB - Subtracts two registers into other the register.
            case 6:
            registers[operation[1]] = registers[operation[2]] - registers[operation[3]];
            break;

            // MUL - Multiplies two registers into other the register.
            case 7:
            registers[operation[1]] = registers[operation[2]] * registers[operation[3]];
            break;

            // DIV - Divides two registers into other the register.
            case 8:
            registers[operation[1]] = registers[operation[2]] / registers[operation[3]];
            break;
            
            // AND - Stores the AND of two registers into the other.
            case 9:
            registers[operation[1]] = (registers[operation[2]] == registers[operation[3]]) ? 1 : 0;
            break;

            // OR - Stores the OR of two registers into the other.
            case 10:
            registers[operation[1]] = (registers[operation[2]] == 1 || registers[operation[3]] == 1) ? 1 : 0;
            break;

            // MOVI - Copy the data from address into register.
            case 11:
            registers[operation[1]] = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // ADDI - Add register by data from address.
            case 12:
            registers[operation[1]] += Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // MULI - Multiply register by data from address.
            case 13:
            registers[operation[1]] = registers[operation[1]] * Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // DIVI - Divide register by data from address.
            case 14:
            registers[operation[1]] = registers[operation[1]] / Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // LDI - Same as MOVI
            case 15:
            registers[operation[1]] = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // SLT - Set register as 1 or 0 on register 2 < register 3.
            case 16:
            registers[operation[1]] = (registers[operation[2]] < registers[operation[3]]) ? 1 : 0;
            break;

            // SLTI - Set register as 1 or 0 on register 2 < addressed data.
            case 17:
            registers[operation[1]] = (registers[operation[2]] < Integer.parseInt(memory.read(operation[3]+dataMemoryOffset)) ? 1 : 0);
            break;
            
            // HTL - Stops the program.
            case 18:
            process.programCounter = process.instructionCount;
            break;

            // NOP - Move to next instruction.
            case 19:
            break;

            // JMP - Jump PC to addressed 
            case 20:
            process.programCounter = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // BEQ - Jumps to address if registers are equal.
            case 21:
            if (registers[operation[1]] == registers[operation[2]])
                process.programCounter = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            break;

            // BNE - Jumps to address if registers are unequal.
            case 22:
            if (registers[operation[1]] != registers[operation[2]])
                process.programCounter = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            break;

            // BEZ - Jumps to address if register is 0.
            case 23:
            if (registers[operation[1]] == 0)
                process.programCounter = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // BNZ - Jumps to address if register is not 0.
            case 24:
            if (registers[operation[1]] != 0)
                process.programCounter = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // BGZ - Jumps to address if register is positive.
            case 25:
            if (registers[operation[1]] > 0)
                process.programCounter = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            // BLZ - Jumps to address if register is negative.
            case 26:
            if (registers[operation[1]] < 0)
                process.programCounter = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;
        }
    }

    // Arbitrary run method that performs the entire CPU data path cycle.
    // Used to make kernel's code simpler.
    void run() {
        fetch();
        int[] operation = decode();
        execute(operation);
    }
}