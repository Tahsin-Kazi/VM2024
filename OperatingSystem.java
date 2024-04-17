import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;


class OperatingSystem {

    MemoryManagementUnit MMU;
    IOController IOC;
    ProcessControlBlock PCB;
    CentralProcessingUnit CPU;
    
    ProcessControlBlock[] jobQueue;
    ProcessControlBlock[] readyQueue;

    void kernel(String programPath) {
        IOC = new IOController();
        MMU = new MemoryManagementUnit();

        jobQueue = new ProcessControlBlock[10];
        readyQueue = new ProcessControlBlock[10];

        loader(programPath);

        longTermScheduler();
        
        CPU = new CentralProcessingUnit(PCB.dataMemoryOffset, MMU, PCB);

        shortTermScheduler();

        while(PCB.programCounter < PCB.instructionCount) {
            CPU.PC = PCB.programCounter;
            CPU.run();
            PCB.programCounter++;
        }
    }

    void loader(String path) {

        File programFile = new File(path);
        Scanner inputPort = new Scanner(programFile);

        while (inputPort.hasNextLine()) {
            IOC.write(inputPort.nextLine());
        }

        inputPort.close();
    }

    void longTermScheduler() {

        for (int i = 0; i < IOC.getUsedSpace(); i++) {
            MMU.write(IOC.read(i));
        }

        if (MMU.read(0).toLowerCase().contains("job")) {
            MMU.delete(0);
        }

        PCB = new ProcessControlBlock();

        for (int i = 0; i < MMU.getUsedSpace(); i++) {
            if (MMU.read(i).toLowerCase().contains("data")) {
                PCB.dataMemoryOffset = i;
                PCB.instructionCount = i;
                MMU.override(i,"data");
            }
        }

        MMU.write("end");

        PCB.pageNumber = 1;
        PCB.programCounter = 0;
        PCB.ready = true;

        jobQueue[0] = PCB;
    }

    void shortTermScheduler() {
        int i = 0;
        while(!jobQueue[i].ready) {
            i++;
        }
        dispatch(0);
    }

    void dispatch(int i) {
        readyQueue[i] = jobQueue[i];
        CPU.PC = PCB.programCounter;
    }
}

class ProcessControlBlock {
    public int programCounter;
    public int dataMemoryOffset;
    public int pageNumber;
    public int instructionCount;
    public boolean ready;
}

class IOController {

    private ArrayList<String> virtualDisc;

    IOController() { 
        virtualDisc = new ArrayList<String>();
    }

    public String read(int i) {
        return virtualDisc.get(i);
    }

    public void write(String s) {
        virtualDisc.add(s);
    }

    public int getUsedSpace() {
        return virtualDisc.size();
    }

}

class MemoryManagementUnit {

    private ArrayList<String> virtualMemory;

    MemoryManagementUnit() {
        virtualMemory = new ArrayList<String>();
    }
    
    public String read(int i) {
        return virtualMemory.get(i);
    }

    public void write(String s) {
        virtualMemory.add(s);
    }
    
    public void write(int i, String s) {
        virtualMemory.add(i, s);
    }

    public void override(int i, String s) {
        virtualMemory.set(i, s);
    }

    public void delete(int i) {
        virtualMemory.remove(i);
    }

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

    CentralProcessingUnit(int offset, MemoryManagementUnit mmu, ProcessControlBlock pcb) {
        
        dataMemoryOffset = offset;

        memory = mmu;

        process = pcb;

        registers = new int[16];

        for(int i = 0; i < 16; i++) {
            registers[i] = 0;
        }
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
        
        if (type == 0) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int reg3 = Integer.parseInt(instruction.substring(16,20));
            int address = Integer.parseInt(instruction.substring(20,24));
            int[] operation = {opcode,reg1,reg2,reg3,address};
            return operation;
        }

        if (type == 1) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int address = Integer.parseInt(instruction.substring(16,32));
            int[] operation = {opcode,reg1,reg2,address};
            return operation;
        }

        if (type == 2) {    
            int address = Integer.parseInt(instruction.substring(8,32));
            int[] operation = {opcode,address};
            return operation;
        }

        if (type == 3) {    
            int reg1 = Integer.parseInt(instruction.substring(8,12));
            int reg2 = Integer.parseInt(instruction.substring(12,16));
            int address = Integer.parseInt(instruction.substring(16,32));
            int[] operation = {opcode,reg1,reg2,address};
            return operation;
        }

        // If the instruction has no valid type, the opcode is returned.
        int[] operation = {opcode};
        return operation;
    }

    // Method used to represent the ALU executing instructions for the data path cycle.
    void execute(int[] operation) {
        // Gets stream of operation and operands from the decode step.
        // Then uses a switch statement to represent all possible opcodes and carries out operation from the opcodes using the proper operands.
        switch(operation[0]) {


            // Uses a case for each opcode and carries out the operation with the operands using the registers and memory access as needed, also checks for the address being null to see if the register or address needs to be used for specific operations.
            // Cases represent opcodes in numerical order as from the assignment table.
                
            case 0:
            if (operation[3]==0) {
                registers[operation[1]] = registers[operation[2]];
            } else {
                registers[operation[1]] = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            }
            break;
            
            case 1:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memory.override(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            case 2:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memory.override(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            case 3:
            if (operation[3]==0) {
                registers[operation[1]] = registers[operation[2]];
            } else {
                registers[operation[1]] = Integer.parseInt(memory.read(operation[3]+dataMemoryOffset));
            }
            break;

            case 4:
            registers[operation[1]] = registers[operation[2]];
            break;

            case 5:
            registers[operation[1]] = registers[operation[2]] + registers[operation[3]];
            break;

            case 6:
            registers[operation[1]] = registers[operation[2]] - registers[operation[3]];
            break;

            case 7:
            registers[operation[1]] = registers[operation[2]] * registers[operation[3]];
            break;

            case 8:
            registers[operation[1]] = registers[operation[2]] / registers[operation[3]];
            break;

            case 9:
            registers[operation[1]] = (registers[operation[2]] == registers[operation[3]]) ? 1 : 0;
            break;

            case 10:
            registers[operation[1]] = (registers[operation[2]] == 1 || registers[operation[3]] == 1) ? 1 : 0;
            break;

            case 11:
            registers[operation[1]] = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            case 12:
            registers[operation[1]] += Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            case 13:
            registers[operation[1]] = registers[operation[1]] * Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            case 14:
            registers[operation[1]] = registers[operation[1]] / Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            case 15:
            registers[operation[1]] = Integer.parseInt(memory.read(operation[2]+dataMemoryOffset));
            break;

            case 16:
            registers[operation[1]] = (registers[operation[2]] < registers[operation[3]]) ? 1 : 0;
            break;

            case 17:
            registers[operation[1]] = (registers[operation[2]] < operation[3]) ? 1 : 0;
            break;

            case 18:
            break;

            case 19:
            break;

            case 20:
            process.programCounter = operation[2];
            break;

            case 21:
            if (registers[operation[1]] == registers[operation[2]])
                process.programCounter = operation[3];
            break;

            case 22:
            if (registers[operation[1]] != registers[operation[2]])
                process.programCounter = operation[3];
            break;

            case 23:
            if (registers[operation[1]] == 0)
                process.programCounter = operation[2];
            break;

            case 24:
            if (registers[operation[1]] != 0)
                process.programCounter = operation[2];
            break;

            case 25:
            if (registers[operation[1]] > 0)
                process.programCounter = operation[2];
            break;

            case 26:
            if (registers[operation[1]] < 0)
                process.programCounter = operation[2];
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
