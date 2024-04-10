import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;


class OperatingSystem {

    // High level variables for the objects controlled by the OS kernel.
    MemoryManager MM;
    ProcessControlBlock PCB;
    CentralProcessingUnit CPU;
    boolean[] readyQueue;

    // Method that serves as the loader for inserting the program file into the disc.
    void loader(String path) {

        // Finds the file and opens an input buffer for it.
        File programFile = new File(path);
        Scanner scan = new Scanner(programFile);

        // Uses the input buffer with the memory manager to add to disc line-by-line.
        while (scan.hasNextLine()) {
            MM.addDisc(scan.nextLine());
        }

    }

    // High level method for the Long Term Scheduler that takes disc program to memory and sets up the PCB.
    void longTermScheduler() {

        // Takes file from disc and adds to memory line-by-line with memory manager.
        for (int i = 0; i < MM.getDiscSize(); i++) {
            MM.addMemory(MM.getDisc(i));
        }

        // Deletes the job line for saving space and readability.
        if (MM.getMemory(0).toLowerCase().contains("job")) {
            MM.deleteMemory(0);
        }

        // Loops through the memory segment to find the data section, count the instructions, and set PCB state.
        for (int i = 0; i < MM.getMemorySize(); i++) {
            if (MM.getMemory(i).toLowerCase().contains("data")) {
                PCB.dataIndex = i;
                PCB.instructionCount = i;
                MM.setMemory(i,"data");
            }
        }

        // Adds a break in memory to signify end of program.
        MM.addMemory("end");

        // Creates a PCB object for the process.
        PCB = new ProcessControlBlock();

        // Sets the program counter to the first instruction (line 0).
        PCB.programCounter = 0;

        // Sets the CPU registers for program execution, making sure the PC and zero register are both zero.
        CPU.registers = new int[16];
        for (int i = 0; i < CPU.registers.length; i++) {
            CPU.registers[i] = 0;
        }

        // Creates a bus between the Memory Manager and CPU
        CPU.memoryAccess = MM;

        // Gives the CPU access to the PCB.
        CPU.PCB = PCB;

        // Sets the CPU's addressing offset for memory accessing.
        CPU.dataMemoryOffset = PCB.dataIndex;

        // Initializes the ready queue for the process.
        readyQueue = new boolean[1];

        // Sets the queue state for the program to be ready.
        readyQueue[0] = true;
    }
}

// A simple object for making PCBs.
class ProcessControlBlock {
    public int programCounter;
    public int dataIndex;
    public int instructionCount;
}

// A high level class to represent the memory manager chip.
class MemoryManager {
    // Two objects to represent the virtual disc and memory, as variable-sized lists of Strings.
    // Array Lists were used to simulate the dynamic functionality of segmentation and IO operations for memory and disc.
    private ArrayList<String> virtualDisc;
    private ArrayList<String> virtualMemory;

    // Constuctor for creating empty virtual disc and memory.
    MemoryManager() {
        virtualDisc = new ArrayList<String>();
        virtualMemory = new ArrayList<String>();
    }

    // Set of methods that carry out various memory/disc operations through the abstraction of the arraylist.
    // Note that Array List operations handle dynamic length, insertion, removal, and compacting to simulate the virtual discs and memory.
    
    public void addMemory(String s) {
        virtualMemory.add(s);
    }

    public void addDisc(String s) {
        virtualDisc.add(s);
    }

    public int getMemorySize() {
        return virtualMemory.size();
    }

    public int getDiscSize() {
        return virtualDisc.size();
    }

    public String getMemory(int i) {
        return virtualMemory.get(i);
    }

    public String getDisc(int i) {
        return virtualDisc.get(i);
    }

    public void deleteMemory(int i) {
        virtualMemory.remove(i);
        return;
    }

    public void setMemory(int i, String s) {
        virtualMemory.set(i, s);
    }
}

// High class to represent the CPU, its features, and its data path cycle.
class CentralProcessingUnit {
    // Set of CPU registers.
    public int[] registers;
    // Offset for Data Addressing
    public int dataMemoryOffset;
    // Access to Memory Manager and PCB.
    public MemoryManager memoryAccess;
    public ProcessControlBlock PCB;

    // Method used to carry out the fetch part of the data path cycle.
    void fetch() {
        // Adds the instruction from the PC to the instruction register for decoding.
        registers[2] = Integer.parseInt(memoryAccess.getMemory(PCB.programCounter));
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
                registers[operation[1]] = Integer.parseInt(memoryAccess.getMemory(operation[3]+dataMemoryOffset));
            }
            break;
            
            case 1:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memoryAccess.setMemory(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            case 2:
            if (operation[3]==0) {
                registers[operation[2]] = registers[operation[1]];
            } else {
                memoryAccess.setMemory(operation[3]+dataMemoryOffset, Integer.toString(registers[operation[1]]));
            }
            break;

            case 3:
            if (operation[3]==0) {
                registers[operation[1]] = registers[operation[2]];
            } else {
                registers[operation[1]] = Integer.parseInt(memoryAccess.getMemory(operation[3]+dataMemoryOffset));
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
            registers[operation[1]] = Integer.parseInt(memoryAccess.getMemory(operation[2]+dataMemoryOffset));
            break;

            case 12:
            registers[operation[1]] += Integer.parseInt(memoryAccess.getMemory(operation[2]+dataMemoryOffset));
            break;

            case 13:
            registers[operation[1]] = registers[operation[1]] * Integer.parseInt(memoryAccess.getMemory(operation[2]+dataMemoryOffset));
            break;

            case 14:
            registers[operation[1]] = registers[operation[1]] / Integer.parseInt(memoryAccess.getMemory(operation[2]+dataMemoryOffset));
            break;

            case 15:
            registers[operation[1]] = Integer.parseInt(memoryAccess.getMemory(operation[2]+dataMemoryOffset));
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
            PCB.programCounter = operation[2];
            break;

            case 21:
            if (registers[operation[1]] == registers[operation[2]])
                PCB.programCounter = operation[3];
            break;

            case 22:
            if (registers[operation[1]] != registers[operation[2]])
                PCB.programCounter = operation[3];
            break;

            case 23:
            if (registers[operation[1]] == 0)
                PCB.programCounter = operation[2];
            break;

            case 24:
            if (registers[operation[1]] != 0)
                PCB.programCounter = operation[2];
            break;

            case 25:
            if (registers[operation[1]] > 0)
                PCB.programCounter = operation[2];
            break;

            case 26:
            if (registers[operation[1]] < 0)
                PCB.programCounter = operation[2];
            break;
        }
    }

    // Arbitrary run method that performs the entire CPU data path cycle from only inputting the PC.
    // Used to make kernel's code simpler.
    void run() {
        fetch();
        int[] operation = decode();
        execute(operation);
    }
}
