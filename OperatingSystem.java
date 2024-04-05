import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

class OperatingSystem {

    MemoryManager MM;
    ProcessControlBlock PCB;
    CentralProcessingUnit CPU;
    boolean[] readyQueue;

    void loader(String path) {
        
        File programFile = new File(path);
        Scanner scan = new Scanner(programFile);
        
        while (scan.hasNextLine()) {
            MM.addDisc(scan.nextLine());
        }

    }

    void longTermScheduler() {
        
        for (int i = 0; i < MM.getDiscSize(); i++) {
            MM.addMemory(MM.getDisc(i));
        }

        if (MM.getMemory(0).toLowerCase().contains("job")) {
            MM.deleteMemory(0);
        }

        for (int i = 0; i < MM.getMemorySize(); i++) {
            if (MM.getMemory(i).toLowerCase().contains("data")) {
                PCB.dataIndex = i;
                PCB.instructionCount = i;
                MM.setMemory(i,"data");
            }
        }

        MM.addMemory("end");

        PCB = new ProcessControlBlock();

        PCB.programCounter = 0;

        CPU.registers = new int[16];

        for (int i = 0; i < CPU.registers.length; i++) {
            CPU.registers[i] = 0;
        }

        CPU.memoryAccess = MM;

        CPU.PCB = PCB;

        CPU.dataMemoryOffset = PCB.dataIndex;

        readyQueue = new boolean[1];

        readyQueue[0] = true;
    }
}

class ProcessControlBlock {
    public int programCounter;
    public int dataIndex;
    public int instructionCount;
}

class MemoryManager {
    private ArrayList<String> virtualDisc;
    private ArrayList<String> virtualMemory;

    MemoryManager() {
        virtualDisc = new ArrayList<String>();
        virtualMemory = new ArrayList<String>();
    }

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

class CentralProcessingUnit {
    public int[] registers;
    public int dataMemoryOffset;
    public MemoryManager memoryAccess;
    public ProcessControlBlock PCB;

    void fetch() {
        registers[2] = Integer.parseInt(memoryAccess.getMemory(PCB.programCounter));
    }

    int[] decode() {
        
        String instruction = Integer.toBinaryString(registers[2]);

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
        switch(operation[0]) {
            
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

    void run() {
        fetch();
        int[] operation = decode();
        execute(operation);
    }
}