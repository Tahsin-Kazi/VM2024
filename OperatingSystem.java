import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner;

class OperatingSystem {
    ArrayList<String> virtualMemory;
    ProcessControlBlock PCB;

    void loader(String path) {
        
        File programFile = new File(path);
        Scanner scan = new Scanner(programFile);
        
        while (scan.hasNextLine()) {
            virtualMemory.add(scan.nextLine());
        }

        scan.close();
    }

    void longTermScheduler() {
        
        if (virtualMemory.get(0).toLowerCase().contains("job")) {
            virtualMemory.remove(0);
        }

        for (int i = 0; i < virtualMemory.size(); i++) {
            if (virtualMemory.get(i).toLowerCase().contains("data")) {
                PCB.dataIndex = i;
                PCB.instructionCount = i;
                virtualMemory.set(i,"data");
            }
        }

        virtualMemory.add("end");

        PCB = new ProcessControlBlock();
    }

    void shortTermScheduler(CentralProcessingUnit CPU) {
        
        CPU.registers = new int[16];

        for (int i = 0; i < CPU.registers.length; i++) {
            CPU.registers[i] = 0;
        }

        CPU.memory = virtualMemory;

        CPU.memoryAddressOffset = PCB.dataIndex;

        PCB.programCounter = 0;
        PCB.ready = true;
    }
}

class ProcessControlBlock {
    
    public int programCounter;
    public boolean ready;
    public int dataIndex;
    public int instructionCount;

    ProcessControlBlock () {
        ready = false;
    }
}

class CentralProcessingUnit {
    public int[] registers;
    public int memoryAddressOffset;
    public ArrayList<String> memory;

    String fetch(int PC) {
        return memory.get(PC);
    }

    int[] decode(String instruction) {
        
        instruction = new BigInteger(instruction.substring(2),16).toString(2);
        
        registers[2] = Integer.parseInt(instruction);

        int type = Integer.parseInt(instruction.substring(0,2));
        int opcode = Integer.parseInt(instruction.substring(2,8));
        
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

        int[] operation = {opcode};
        return operation;
    }

    void execute(int[] operation) {
    }

    void run(int PC) {
        String instruction = fetch(PC);
        int[] operation = decode(instruction);
        execute(operation);
    }
}