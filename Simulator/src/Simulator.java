import java.io.File;
import java.util.*;
enum Opcode {
    ADD,
    ADDI,
    SUB,
    MUL,
    CMP,
    LD,
    MOVA,
    MOVB,
    LDI,
    MOV,
    ST,
    BR,
    BZ,
    BLEQ,
    AND,
    NOT,
    HALT,
    DIV
}

public class Simulator {
    //
    public Simulator() {
        for (int i = 0; i < 64; i++) {
            registers.add(0);
            arf.add(0);
            scoreboard.put(i, true);
        }
        for (int i = 0; i < 512; i++) {
            memory.add(0);
        }
    }

    public boolean finished = false;
    public int instructionsCount = 0;
    public int PC = 0;
    public boolean isStalled = false;
    public int cycles = 0;
    public ArrayList<Integer> arf = new ArrayList<>();
    public BranchTargetBuffer branchTargetBuffer = new BranchTargetBuffer();
    public ArrayList<ExecutionObj> aluResStat = new ArrayList<>();
    public ArrayList<ExecutionObj> memAccessResStat = new ArrayList<>();
    public ArrayList<ExecutionObj> readyInstructions = new ArrayList<>();
    public ArrayList<ExecutionObj> branchUnitResStat = new ArrayList<>();

    public ArrayList<Integer> registers = new ArrayList<>();
    public HashMap<Integer, Boolean> scoreboard = new HashMap<>();
    public ArrayList<Integer> memory = new ArrayList<>();
    public ArrayList<Instruction> instructions = new ArrayList<>();
    public ArrayList<WriteBackObj> writeBackObjs = new ArrayList<>();

    public ArrayList<Instruction> fetchQueue = new ArrayList<>();
    public ArrayList<ExecutionObj> decodeQueue = new ArrayList<>();


    public ExecutionState executionState = null;
    public ExecutionState branchUnitState = null;
    public ExecutionState aluState = null;
    public ExecutionState alu2State = null;
    public ExecutionState memAccessState = null;
    public Checkpoint lastCheckpoint = new Checkpoint(0, this.registers, this.memory,
            new ExecutionObj(Opcode.HALT,0,0,0,0,0,0),false);

    public void initialise() {
        //preprocess inputs
        try {
            File myFile = new File("resources/input.txt");
            Scanner sc = new Scanner(myFile);
            int lineNumber = 0;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] parts = line.split(" ");
                Instruction instruction = new Instruction(lineNumber, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
                this.instructions.add(instruction);
                lineNumber++;
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("please name the input file:- \"input.txt\"");
        }

        this.cycles += 2;
        clock();
    }

    public void clock() {
        while (!this.finished) {
            for (int i = 0; i < 1; i++) {
                if (!this.isStalled) {
                    fetch();
                    decode();
                    issue();
                }
                runExecutionUnits();
                writeback();
            }
            this.cycles++;
        }
        this.arf = this.registers;
    }

    public void writeback()
    {
        for (WriteBackObj obj :this.writeBackObjs) {
            this.registers.set(obj.register, obj.value);
        }
        this.writeBackObjs = new ArrayList<>();
    }

    public void runExecutionUnits()
    {
        alu();
        alu2();
        memoryAccess();
        branchUnit();
    }

    public void fetch() {
        System.out.println("fetching");

        Instruction instruction = this.instructions.get(this.PC);
        System.out.println(instruction.opcode);
        if(isBranch(instruction.opcode))
        {
            //send to branch predictor.
            branchPredictor(fastDecode(instruction));
        }
        fetchQueue.add(instruction);
        //return instruction;
    }

    public void decode() {
        // if fetchqueue empty, return.
        if(this.fetchQueue.isEmpty())
        {
            return;
        }
        Instruction instruction = this.fetchQueue.get(0);
        this.fetchQueue.remove(0);
        System.out.println("decode");
        //Opcode opcode = Opcode.HALT;
        int r1 = -1;
        int r2 = -1;
        int memory_address = -1;
        int target_address = -1;
        int resultRegister = -1;
        int constant = -1;
        switch (instruction.opcode) {
            case BR: // BR ADDRESS
                target_address = Integer.parseInt(instruction.operands[0]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BZ: // BZ R1 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                target_address = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case LD: // LD R1 MEM_ADDRESS TODO
                resultRegister = Integer.parseInt(instruction.operands[0]);
                memory_address = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case MOVB:
                resultRegister = this.registers.get(Integer.parseInt(instruction.operands[0]));
                r1 = this.registers.get(Integer.parseInt(instruction.operands[1]));
                break;
            case MOVA:
                resultRegister = Integer.parseInt(instruction.operands[0]);
                r1 = Integer.parseInt(instruction.operands[1]);
                break;
            case ST: // ST R1 R2
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case ADD: // ADD R1 R2 REGRESULT
            case MUL: //MUL R1 R2 REGRESULT
            case AND: //AND R1 R2 REGRESULT
            case CMP: //CMP R1 R2 REGRESULT
            case SUB: //SUB R1 R2 REGRESULT
            case DIV:
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                resultRegister = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case LDI: //LDI REGRESULT CONST
                resultRegister = Integer.parseInt(instruction.operands[0]);
                constant = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case MOV: //MOV R1 REGRESULT
            case NOT: //NOT R1 REGRESULT
                r1 = Integer.parseInt(instruction.operands[0]);
                resultRegister = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case ADDI: //ADD R1 CONST REGRESULT
                r1 = Integer.parseInt(instruction.operands[0]);
                constant = Integer.parseInt(instruction.operands[1]);
                resultRegister = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BLEQ: //BLEQ R1 R2 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                target_address = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case HALT: // HALT
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            default:
                System.out.println("Error, unknown OPCODE when decoding");
                break;
        }
        this.decodeQueue.add(new ExecutionObj(instruction.opcode, constant, resultRegister, r1, r2,
                target_address, memory_address));

    }

    public ExecutionObj fastDecode(Instruction instruction) {
        System.out.println("fastDecode");
        int r1 = -1;
        int r2 = -1;
        int memory_address = -1;
        int target_address = -1;
        int resultRegister = -1;
        int constant = -1;
        switch (instruction.opcode) {
            case BR: // BR ADDRESS
                target_address = Integer.parseInt(instruction.operands[0]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BZ: // BZ R1 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                target_address = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BLEQ: //BLEQ R1 R2 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                target_address = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            default:
                System.out.println("Error, only send branches to fast decode");
                break;
        }
        return new ExecutionObj(instruction.opcode, constant, resultRegister, r1, r2,
                target_address, memory_address);

    }
    public void issue() {
        // if decodequeue empty, return.
        if(this.decodeQueue.isEmpty())
        {
            return;
        }
        ExecutionObj executionObj = this.decodeQueue.get(0);
        this.decodeQueue.remove(0);
        System.out.println("issue");
        boolean isAluInstruction = isAluInstruction(executionObj.opcode);
        if (isAluInstruction) {
            this.aluResStat.add(executionObj);
        } else if(isBranch(executionObj.opcode))
        {
            this.branchUnitResStat.add(executionObj);
        }
        else{
            this.memAccessResStat.add(executionObj);
        }
    }

    public void executionUnit() {
        if (this.executionState == null) {
            this.executionState = new ExecutionState(this.readyInstructions.get(0));
            this.readyInstructions.remove(0);
        }
        executionState.currentCycleNumber++;
        if (executionState.isComplete()) {
            execute(executionState.executionObj);
            this.executionState = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }


    }

    public void execute(ExecutionObj executionObj) {
        System.out.println("executing");
        int newPC = 0;
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;
        this.instructionsCount++;
        boolean sameAsLastCheckpoint = false;
        if(this.lastCheckpoint.executionObj.equals(executionObj))
        {
            sameAsLastCheckpoint = true;
        }

        switch (opcode) {// BR ADDRESS
            case BR: {
                newPC = target_address;
                break;
            }
            case BZ: {// BZ R1 ADDRESS
                if (this.registers.get(r1) == 0) {

                    if(sameAsLastCheckpoint && !this.lastCheckpoint.branchTaken)
                    {
                        //made incorrect choice so flush the pipeline
                        flush();
                    }
                    else {
                        //we are certain this is correct so commit it to the arf :)
                        commit();
                    }
                    newPC = target_address;
                }
                else {
                    if (sameAsLastCheckpoint && this.lastCheckpoint.branchTaken) {
                        //made incorrect choice so flush the pipeline
                        flush();
                    }
                    else {
                        //we are certain this is correct so commit it to the arf :)
                        commit();
                    }
                    newPC = this.lastCheckpoint.PC + 1;
                }
                break;
            }
// LD R1 MEM_ADDRESS
            case LD: {
                this.registers.set(resultRegister, this.registers.get(memory_address));
                newPC = this.PC + 1;
                break;
            }
// MOVA RESULTREG R1
            case MOVA: {
                this.registers.set(resultRegister, this.registers.get(this.registers.get(r1)));
                newPC = this.PC + 1;
                break;
            }
// MOVB R1 R2
            case MOVB: {
                this.registers.set(resultRegister, r1);
                newPC = this.PC + 1;
                break;
            }
// ST R1 R2
            case ST: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.memory.set(first, second);
                newPC = this.PC + 1;
                break;
            }
// ADD R1 R2 REGRESULT
            case ADD: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, first + second)));
                //this.registers.set(resultRegister, (first + second));
                System.out.println("added registers :- " + r1 + " and " + r2 + " to get " + this.registers.get(resultRegister));
                newPC = this.PC + 1;
                break;
            }
//SUB R1 R2 REGRESULT
            case SUB: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, first - second)));
                //this.registers.set(resultRegister, first - second);
                newPC = this.PC + 1;
                break;
            }
//CMP R1 R2 REGRESULT
            case CMP: {
                int value;
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                if (first > second) {
                    value = 1;
                    //this.registers.set(resultRegister, 1);
                } else if (first == second) {
                    value = 0;
                    //this.registers.set(resultRegister, 0);
                } else {
                    value = -1;
                    //this.registers.set(resultRegister, -1);
                }
                this.writeBackObjs.add((new WriteBackObj(resultRegister, value)));
                newPC = this.PC + 1;
                break;
            }
//AND R1 R2 REGRESULT
            case AND: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, first & second)));
                //this.registers.set(resultRegister, first & second);
                newPC = this.PC + 1;
                break;
            }
//LDI REGRESULT CONST
            case LDI: {
                this.registers.set(resultRegister, constant);
                System.out.println("set register :- " + resultRegister + " to " + constant);
                newPC = this.PC + 1;
                break;
            }
//MOV R1 REGRESULT
            case MOV: {
                int first = this.registers.get(r1);
                this.registers.set(resultRegister, first);
                newPC = this.PC + 1;
                break;
            }
//MUL R1 R2 REGRESULT
            case MUL: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, first * second)));
                //this.registers.set(resultRegister, first * second);
                newPC = this.PC + 1;
                break;
            }
//NOT R1 REGRESULT
            case NOT: {
                int first = this.registers.get(r1);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, ~first)));
                //this.registers.set(resultRegister, ~first);
                newPC = this.PC + 1;
                break;
            }
//ADD R1 CONST REGRESULT
            case ADDI: {
                int value = this.registers.get(r1);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, value + constant)));
                //this.registers.set(resultRegister, value + constant);
                newPC = this.PC + 1;
                break;
            }
//BLEQ R1 R2 ADDRESS
            case BLEQ: {

                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                if (first <= second) {
                    if(sameAsLastCheckpoint && !this.lastCheckpoint.branchTaken)
                    {
                        flush();
                    }
                    else {
                        commit();
                    }
                    newPC = target_address;
                } else {
                    if (sameAsLastCheckpoint && this.lastCheckpoint.branchTaken) {
                        flush();
                    }
                    else {
                        commit();
                    }
                    newPC = this.lastCheckpoint.PC + 1;

                }
                break;
            }
// HALT
            case HALT: {
                this.finished = true;
                System.out.println("set finished to true inside of execute");
                System.out.println(executionObj.opcode);
                break;
            }
            case DIV: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.writeBackObjs.add((new WriteBackObj(resultRegister, first / second)));
                newPC = this.PC + 1;
                break;
            }
            default:
                System.out.println("Error, unknown OPCODE in execute");
                break;
        }
        this.PC = newPC;
    }

    public void alu() {
        if (this.aluState == null) {
            if (this.aluResStat.size() > 0) {
                for(int i = 0; i < this.aluResStat.size(); i++)
                {
                    if(isAvailable(aluResStat.get(i)))
                    {
                        this.aluState = new ExecutionState(this.aluResStat.get(i));
                        this.aluResStat.remove(i);
                        break;
                    }
                }

            } else {
                //System.out.println("alu queue empty");
                return;
            }
        }
        if(this.aluState == null)
        {
            return;
        }

        aluState.currentCycleNumber++;
        if (aluState.isComplete()) {
            execute(aluState.executionObj);
            this.aluState = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }
    }

    public void branchUnit() {
        if (this.branchUnitState == null) {
            if (this.branchUnitResStat.size() > 0) {
                for(int i = 0; i < this.branchUnitResStat.size(); i++)
                {
                    if(isAvailable(branchUnitResStat.get(i)))
                    {
                        this.branchUnitState = new ExecutionState(this.branchUnitResStat.get(i));
                        this.branchUnitResStat.remove(i);
                        break;
                    }
                }

            } else {
                //System.out.println("alu queue empty");
                return;
            }
        }
        if(this.branchUnitState == null)
        {
            return;
        }

        branchUnitState.currentCycleNumber++;
        if (branchUnitState.isComplete()) {
            execute(branchUnitState.executionObj);
            this.branchUnitState = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }
    }

    public void alu2() {
        if (this.alu2State == null) {
            if (this.aluResStat.size() > 0) {
                for(int i = 0; i < this.aluResStat.size(); i++)
                {
                    if(isAvailable(aluResStat.get(i)))
                    {
                        this.alu2State = new ExecutionState(this.aluResStat.get(i));
                        this.aluResStat.remove(i);
                        break;
                    }
                }

            } else {
                //System.out.println("alu queue empty");
                return;
            }
        }
        if(this.alu2State == null)
        {
            return;
        }

        alu2State.currentCycleNumber++;
        if (alu2State.isComplete()) {
            execute(alu2State.executionObj);
            this.alu2State = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }
    }

    public void memoryAccess() {
        if (this.memAccessState == null) {
            if (memAccessResStat.size() > 0) {
                for(int i = 0; i < this.memAccessResStat.size(); i++)
                {
                    if(isAvailable(memAccessResStat.get(i)))
                    {
                        this.memAccessState = new ExecutionState(this.memAccessResStat.get(i));
                        this.memAccessResStat.remove(i);
                        break;
                    }
                }
            } else {
                //System.out.println("memory access queue is empty");
                return;
            }
        }

        if(this.memAccessState == null)
        {
            return;
        }

        memAccessState.currentCycleNumber++;
        if (memAccessState.isComplete()) {
            execute(memAccessState.executionObj);
            this.memAccessState = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }
    }

    public void branchPredictor(ExecutionObj executionObj) {
        //takes in a branch instruction as an input. needs to set a checkpoint that it can revert to if incorrect
        // if (target_address > this.PC) don't take branch else take branch for a static implementation. Can create
        // a cache of known branches to implement dynamic branch prediction at a later date.
        int target_address = executionObj.target_address;

        int checkpointPC = this.PC;
        ArrayList<Integer> checkPointRegisters = this.registers;
        ArrayList<Integer> checkPointMem = this.memory;
        if(branchTargetBuffer.contains(this.PC))
        {
            if(branchTargetBuffer.get(this.PC).taken >=2)
            {
                this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, true);
                this.PC = target_address;
            }
            else {
                this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, false);
                this.incrementPC();
            }
        }
        //always take backwards branches if entry is not found.
        else if (target_address > this.PC) {
            branchTargetBuffer.entries.add(new BranchEntry(this.PC, 1, target_address));
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, false);
            this.incrementPC();
        } else {
            branchTargetBuffer.entries.add(new BranchEntry(this.PC, 2, target_address));
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, true);
            this.PC = target_address;
        }
    }

    private void flush()
    {
        if(this.branchTargetBuffer.contains(this.lastCheckpoint.PC))
        {
            if(this.branchTargetBuffer.get(this.lastCheckpoint.PC).taken > 0)
            {
                this.branchTargetBuffer.get(this.lastCheckpoint.PC).taken--;
            }
        }

        this.registers = this.lastCheckpoint.registers;
        this.memory = this.lastCheckpoint.memory;
        this.fetchQueue.clear();
        this.decodeQueue.clear();
        this.aluState = null;
        this.memAccessState = null;
        this.aluResStat.clear();
        this.memAccessResStat.clear();
        this.branchUnitResStat.clear();
        this.cycles += 2;
    }

    private void commit()
    {
        if(this.branchTargetBuffer.contains(this.lastCheckpoint.PC)) {
            if (this.branchTargetBuffer.get(this.lastCheckpoint.PC).taken < 3) {
                this.branchTargetBuffer.get(this.lastCheckpoint.PC).taken++;
            }
        }

        this.arf = this.registers;
    }
    public boolean isAvailable(ExecutionObj obj)
    {
        int r1 = obj.r1;
        int r2 = obj.r2;
        int resultReg = obj.resultRegister;
        boolean isAvailable = true;
        if(r1 != -1 && !scoreboard.get(r1))
        {
            isAvailable = false;
        } else if (r2 != -1 && !scoreboard.get(r2)) {
            isAvailable = false;
        }
        else if (resultReg != -1 && !scoreboard.get(resultReg))
        {
            isAvailable = false;
        }
        return isAvailable;
    }

    private boolean isAluInstruction(Opcode opcode) {
        System.out.println("entered into alu function");
        switch (opcode) {
            case DIV:
            case CMP:
            case MUL:
            case ADD:
            case SUB:
            case AND:
            case NOT:
            case ADDI:
            {
                return true;
            }
            default:
                return false;
        }
    }

    private boolean isBranch(Opcode opcode)
    {
        switch (opcode) {
            case BZ:
            case BLEQ:
            case BR:
            {
                return true;
            }
            default:
                return false;
        }
    }
    private void incrementPC() {
        this.PC = this.PC + 1;
    }
}



/*

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.CheckedOutputStream;


enum Opcode {
    ADD,
    ADDI,
    SUB,
    MUL,
    CMP,
    MOVA,
    MOVB,
    LD,
    LDI,
    MOV,
    ST,
    BR,
    BZ,
    BLEQ,
    AND,
    NOT,
    HALT,
    DIV
}

public class Simulator {
    //
    public Simulator() {
        for(int i = 0; i < 64; i++)
        {
            registers.add(0);
        }
        for(int i = 0; i < 64; i++)
        {
            registerFile.add(0);
        }
        for(int i = 0; i < 512; i++)
        {
            memory.add(0);
        }
    }
    public boolean finished = false;
    public int instructionsCount = 0;
    public int PC = 0;
    public int cycles = 0;
    public ArrayList<Integer> registers = new ArrayList<>();
    public ArrayList<Integer> registerFile = new ArrayList<>();
    public ArrayList<Integer> memory = new ArrayList<>();
    public ArrayList<Instruction> instructions = new ArrayList<>();
    public Thread fetchThread;
    public Thread decodeThread;
    public Thread executeThread;
    public Thread clockThread;
    public Checkpoint lastCheckpoint;


    public void shutdown() {
        this.clockThread.interrupt();
        this.clockThread.stop();

        this.fetchThread.interrupt();
        this.fetchThread.stop();

        this.decodeThread.interrupt();
        this.decodeThread.stop();

        this.executeThread.interrupt();
        this.executeThread.stop();
    }
    public void initialise() throws InterruptedException {
        //preprocess inputs
        try {
            File myFile = new File("resources/input.txt");
            Scanner sc = new Scanner(myFile);
            int lineNumber = 0;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] parts = line.split(" ");
                Instruction instruction = new Instruction(lineNumber, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
                this.instructions.add(instruction);
                lineNumber++;
            }
            sc.close();
        }
        catch(Exception e){
            System.out.println("please name the input file:- \"input.txt\"");
        }

        BlockingQueue<String> fetchQueue = new ArrayBlockingQueue<>(5);
        BlockingQueue<Instruction> decodeQueue = new ArrayBlockingQueue<>(5);
        BlockingQueue<ExecutionObj> executeQueue = new ArrayBlockingQueue<>(5);

        //queues for clock to know to tick again
        BlockingQueue<String> fetchClockQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<String> decodeClockQueue = new ArrayBlockingQueue<>(1);
        BlockingQueue<String> executeClockQueue = new ArrayBlockingQueue<>(1);



        //TODO check if other parts have stalled before putting messages in their queue
        //TODO issue stage to different execution units
        //all instructions take one cycle so not necessary yet.
        //TODO make instructions take more than one cycle.

        this.clockThread = new Thread(() -> {
            while (!this.finished) {
                System.out.println("Clock tick " + this.cycles++);
                try {
                    fetchQueue.put("Fetch");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    String message = fetchClockQueue.take();
                    String message2 = decodeClockQueue.take();
                    String message3 = executeClockQueue.take();
                }
                catch (Exception e)
                {
                    System.out.println("error receiving messages in the clock");
                }
            }
            System.out.println("clock has broken out of its loop");
            shutdown();
        });

        this.fetchThread = new Thread(() -> {
            while (!this.finished) {
                try {
                    String message = fetchQueue.take();
                    System.out.println("Fetching... " + message);
                    if (this.PC < this.instructions.size())
                    {
                        Instruction instruction = this.instructions.get(this.PC);
                        decodeQueue.put(instruction);
                        if(instruction.opcode == Opcode.HALT)
                        {
                            break;
                        }
                    }
                    else {
                        this.finished = true;
                        System.out.println("set finished to true");
                    }
                    fetchClockQueue.put("tick please!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("fetch has broken out of its loop");
            shutdown();
        });

        this.decodeThread = new Thread(() -> {
            while (!this.finished) {
                try {
                    Instruction instruction = decodeQueue.take();
                    ExecutionObj executionObj = decode(instruction);
                    System.out.println("Decode received message: ");
                    executeQueue.put(executionObj);
                    if(instruction.opcode == Opcode.HALT)
                    {
                        break;
                    }
                    decodeClockQueue.put("tick please!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("decode has broken out of its loop");
            shutdown();
        });

        this.executeThread = new Thread(() -> {
            while (!this.finished) {
                try {
                    ExecutionObj executionObj = executeQueue.take();
                    execute(executionObj);
                    System.out.println("Execution received message");
                    if(executionObj.opcode == Opcode.HALT)
                    {
                        break;
                    }
                    executeClockQueue.put("tick please!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("execute has broken out of its loop");

            shutdown();
            //Thread.currentThread().interrupt();
            //executeThread.stop();
        });

        clockThread.start();
        fetchThread.start();
        decodeThread.start();
        executeThread.start();

        clockThread.join();
        fetchThread.join();
        decodeThread.join();
        executeThread.join();
    }


    public void fetch(){
        if (this.PC < this.instructions.size())
        {
            decode(this.instructions.get(this.PC));
        }
        else {
            System.out.println("invalid program counter");
            this.finished = true;
            System.out.println("set finished to true");
        }

    }

    public ExecutionObj issue(ExecutionObj executionObj)
    {
        //TODO switch statement based on the opcode.
        return null;
    }
    public ExecutionObj decode(Instruction instruction){
        //System.out.println("decode");
        //Opcode opcode = Opcode.HALT;
        int r1 = -1;
        int r2 = -1;
        int memory_address = -1;
        int target_address = -1;
        int resultRegister = -1;
        int constant = -1;
        switch(instruction.opcode) {
            case BR: // BR ADDRESS
                target_address = Integer.parseInt(instruction.operands[0]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BZ: // BZ R1 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                target_address = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case LD: // LD R1 MEM_ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                memory_address = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case MOVB:
            case MOVA://MOVA R1 R2
            case ST: // ST R1 R2
                r1 =  Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                break;
            //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
            case ADD: // ADD R1 R2 REGRESULT
            case MUL: //MUL R1 R2 REGRESULT
            case AND: //AND R1 R2 REGRESULT
            case CMP: //CMP R1 R2 REGRESULT
            case SUB: //SUB R1 R2 REGRESULT
            case DIV: //DIV R1 R2 REGRESULT
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                resultRegister = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case LDI: //LDI REGRESULT CONST
                resultRegister = Integer.parseInt(instruction.operands[0]);
                constant = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case MOV: //MOV R1 REGRESULT
            case NOT: //NOT R1 REGRESULT
                r1 = Integer.parseInt(instruction.operands[0]);
                resultRegister = Integer.parseInt(instruction.operands[1]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case ADDI: //ADD R1 CONST REGRESULT
                r1 = Integer.parseInt(instruction.operands[0]);
                constant = Integer.parseInt(instruction.operands[1]);
                resultRegister = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case BLEQ: //BLEQ R1 R2 ADDRESS
                r1 = Integer.parseInt(instruction.operands[0]);
                r2 = Integer.parseInt(instruction.operands[1]);
                target_address = Integer.parseInt(instruction.operands[2]);
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            case HALT: // HALT
                //execute(instruction.opcode, constant, resultRegister, r1, r2, target_address, memory_address);
                break;
            default:
                System.out.println("Error, unknown OPCODE when decoding");
                break;
        }
        return new ExecutionObj(instruction.opcode, constant, resultRegister, r1, r2,
                target_address, memory_address);

    }

    public int execute(ExecutionObj executionObj){
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int  resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;
        //TODO this should send a pair to set in memory to writeback
        this.instructionsCount++;
        switch(opcode){
            case BR: {// BR ADDRESS
                this.PC = target_address;
                return 0;
            }
            case BZ: {// BZ R1 ADDRESS
                if (this.registers.get(r1) == 0)
                    this.PC = target_address;
                else
                    this.incrementPC();
                return 0;
            }
            case LD: {// LD R1 MEM_ADDRESS
                this.registers.set(r1, this.registers.get(memory_address));
                this.incrementPC();
                return 0;
            }
            case MOVA: {// MOVA R1 R2
                this.registers.set(r1, this.registers.get(this.registers.get(r2)));
                this.incrementPC();
                return 0;
            }
            case MOVB: {// MOVB R1 R2
                this.registers.set(this.registers.get(r1), this.registers.get(r2));
                this.incrementPC();
                return 0;
            }

            case ST: {// ST R1 R2
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.memory.set(first, second);
                this.incrementPC();
                return 0;
            }
            case ADD: {// ADD R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, (first + second));
                System.out.println("added registers :- " + r1 + " and " + r2 + " to get " + this.registers.get(resultRegister));

                this.incrementPC();
                return 0;
            }
            case SUB: {//SUB R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first - second);
                this.incrementPC();
                return 0;
            }

            case CMP: {//CMP R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);

                if (first > second) {
                    this.registers.set(resultRegister, 1);
                } else if (first == second) {
                    this.registers.set(resultRegister, 0);
                } else {
                    this.registers.set(resultRegister, -1);
                }
                this.incrementPC();

                return 0;
            }
            case AND: {//AND R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first & second);

                return 0;
            }
            case LDI: {//LDI REGRESULT CONST
                this.registers.set(resultRegister, constant);
                System.out.println("set register :- " + resultRegister + " to " + constant);
                this.incrementPC();
                return 0;
            }
            case MOV: {//MOV R1 REGRESULT
                int first = this.registers.get(r1);
                this.registers.set(resultRegister, first);
                return 0;
            }
            case MUL: {//MUL R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first * second);
                return 0;
            }
            case NOT: {//NOT R1 REGRESULT
                int first = this.registers.get(r1);
                this.registers.set(resultRegister, ~first);
                return 0;
            }
            case ADDI: {//ADD R1 CONST REGRESULT
                int value = this.registers.get(r1);
                this.registers.set(resultRegister, value + constant);
                this.incrementPC();
                return 0;
            }
            case BLEQ: {//BLEQ R1 R2 ADDRESS
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                if (first <= second) {
                    this.PC = target_address;
                } else {
                    this.incrementPC();
                }
                return 0;
            }
            case HALT: {// HALT
                this.finished = true;
                System.out.println("set finished to true");
                return 0;
            }
            default:
                System.out.println("Error, unknown OPCODE");
                return 1;
        }
    }

    public int alu(ExecutionObj executionObj)
    {
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int  resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;
        //process all arithmetic with integers in here. Some instructions may take several cycles
        switch(opcode)
        {
            case ADD: {// ADD R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, (first + second));
                System.out.println("added registers :- " + r1 + " and " + r2 + " to get " + this.registers.get(resultRegister));

                this.incrementPC();
                return 0;
            }
            case SUB: {//SUB R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first - second);
                this.incrementPC();
                return 0;
            }
            case MUL: {//MUL R1 R2 REGRESULT
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first * second);
                return 0;
            }
            case ADDI: {//ADD R1 CONST REGRESULT
                int value = this.registers.get(r1);
                this.registers.set(resultRegister, value + constant);
                this.incrementPC();
                return 0;
            }
            default: {
                System.out.println("alu was wrongly assigned an instruction");
                return 1;
            }
        }

    }

    public void branchPredictor(ExecutionObj executionObj)
    {
        //takes in a branch instruction as an input. needs to set a checkpoint that it can revert to if incorrect
        // if (target_address > this.PC) don't take branch else take branch for a static implementation. Can create
        // a cache of known branches to implement dynamic branch prediction at a later date.
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int  resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;

        int checkpointPC = this.PC;
        ArrayList<Integer> checkPointRegisters = this.registers;
        ArrayList<Integer> checkPointMem = this.memory;
        //always take backwards branches
        if(target_address > this.PC)
        {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, false);
            this.incrementPC();
        }
        else {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, true);
            this.PC = target_address;
        }
        //TODO actually calculate the value of the branch. I think that we can check every branch instruction against executionObj
        //and it's actual value. If this is different. reload the last checkpoint.


    }

    public void floatingALU()
    {

    }

    public void writeBack()
    {
        for (int i = 0; i < registers.size(); i++) {
            if(!Objects.equals(registers.get(i), registerFile.get(i)))
            {
                registerFile.set(i, registers.get(i));
            }
        }

        //TODO receive messages from execute to set memory.
    }
    private void incrementPC() {
        this.PC = this.PC + 1;
    }
}


/* Defining ISA

add - 0000 adds two operands into a destination register.
addi - 0001
cmp - 0010
ld - 0011
ldi - 0100
br - 0101
ju - 0110
bleq - 0111
and - 1000
not - 1001
mov - 1010
mul - 1011
bz - 1100
 */
