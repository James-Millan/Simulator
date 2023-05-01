import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.CheckedOutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;




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
        }
        for (int i = 0; i < 64; i++) {
            registerFile.add(0);
        }
        for (int i = 0; i < 512; i++) {
            memory.add(0);
        }
    }

    public boolean finished = false;
    public int lastCompletedInstruction = -1;
    private final Lock pcLock = new ReentrantLock(true);
    public int instructionsCount = 0;
    public int PC = 0;
    public boolean isStalled = false;
    public int cycles = 0;
    public ArrayList<ExecutionObj> waitingInstructions = new ArrayList<>();
    public ArrayList<ExecutionObj> readyInstructions = new ArrayList<>();
    public ArrayList<Integer> registers = new ArrayList<>();
    public ArrayList<Integer> registerFile = new ArrayList<>();
    public ArrayList<Integer> memory = new ArrayList<>();
    public ArrayList<Instruction> instructions = new ArrayList<>();

    public Instruction fetchedInstruction = null;
    public ExecutionObj decodedInstruction = null;

    public ExecutionState executionState = null;
    public Checkpoint lastCheckpoint;

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
        clock();


        //later on check if other parts have stalled before putting messages in their queue not gonna do this yet as PC doesn't
        //change in a scalar processor when it has stalled.
        // issue stage to different execution units
        //TODO update execution threads now... Remove executed instruction from dependencies. If dependency count goes to 0 move to ready.
        //TODO 2 ALU, 1 memory access, 1 branch predictor



//        this.alu1Thread = new Thread(() -> {
//            ExecutionState executionState = new ExecutionState(new ExecutionObj(Opcode.DIV, 0, 0, 0, 0, 0, 0));
//            boolean finishedCurrentInstruction = true;
//            boolean initialised = false;
//            while (!this.finished) {
//                try {
//                    alu1ClockQueue.take();
//                    ExecutionObj executionObj = executionState.executionObj;
//
//                    if (finishedCurrentInstruction) {
//                        //set boolean after taking message for the first time.
//                        if (alu1Queue.isEmpty()) {
//                            //System.out.println("empty queue so ticking!");
//                            alu1ClockQueue.put("tick please!");
//                            continue;
//                        }
//                        finishedCurrentInstruction = false;
//                        executionObj = alu1Queue.take();
//                        initialised = true;
//                        System.out.println("alu1 received message");
//                        executionState = new ExecutionState(executionObj);
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                            this.lastCompletedInstruction = executionObj.instructionID;
//                        }
//                    } else {
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    if (executionObj.opcode == Opcode.HALT) {
//                        break;
//                    }
//                    //System.out.println("alu1 finished one cycle");
//                    alu1ClockQueue.put("tick please!");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("alu1 has broken out of its loop");
//
//            shutdown();
//            //Thread.currentThread().interrupt();
//            //executeThread.stop();
//        });
//        this.alu2Thread = new Thread(() -> {
//            ExecutionState executionState = new ExecutionState(new ExecutionObj(Opcode.DIV, 0, 0, 0, 0, 0, 0));
//            boolean finishedCurrentInstruction = true;
//            boolean initialised = false;
//            while (!this.finished) {
//                try {
//                    alu2ClockQueue.take();
//                    ExecutionObj executionObj = executionState.executionObj;
//
//                    if (finishedCurrentInstruction) {
//                        if (alu2Queue.isEmpty()) {
//                            alu2ClockQueue.put("tick please!");
//                            continue;
//                        }
//                        finishedCurrentInstruction = false;
//                        executionObj = alu2Queue.take();
//                        initialised = true;
//                        System.out.println("alu2 received message");
//                        executionState = new ExecutionState(executionObj);
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                            this.lastCompletedInstruction = executionObj.instructionID;
//                        }
//                    } else {
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                            this.lastCompletedInstruction = executionObj.instructionID;
//                        }
//                    }
//
//
//                    if (executionObj.opcode == Opcode.HALT) {
//                        break;
//                    }
//                    //System.out.println("alu2 finished one cycle");
//
//                    alu2ClockQueue.put("tick please!");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("alu2 has broken out of its loop");
//            shutdown();
//        });
//        this.memAccessThread = new Thread(() -> {
//            ExecutionState executionState = new ExecutionState(new ExecutionObj(Opcode.DIV, 0, 0, 0, 0, 0, 0));
//            boolean finishedCurrentInstruction = true;
//            boolean initialised = false;
//            while (!this.finished) {
//                try {
//                    memAccessClockQueue.take();
//                    ExecutionObj executionObj = executionState.executionObj;
//
//                    if (finishedCurrentInstruction) {
//                        if (memAccessQueue.isEmpty()) {
//                            memAccessClockQueue.put("tick please!");
//                            continue;
//                        }
//                        finishedCurrentInstruction = false;
//                        executionObj = memAccessQueue.take();
//                        initialised = true;
//                        System.out.println("memory access received message");
//                        executionState = new ExecutionState(executionObj);
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                            this.lastCompletedInstruction = executionObj.instructionID;
//                        }
//                    } else {
//                        executionState.currentCycleNumber++;
//                        if (executionState.isComplete()) {
//                            finishedCurrentInstruction = true;
//                            execute(executionObj);
//                            for (int i = 0; i < this.waitingInstructions.size(); i++) {
//                                ExecutionObj execObj = this.waitingInstructions.get(i);
//                                for (int j = 0; j < execObj.dependencies.size(); j++) {
//                                    if (execObj.dependencies.get(j) == executionObj.instructionID) {
//                                        //remove the integer from the list not the index.
//                                        execObj.dependencies.remove(j);
//                                        j--;
//                                        if (execObj.dependencies.size() == 0) {
//                                            this.readyInstructions.add(execObj);
//                                            this.waitingInstructions.remove(execObj);
//                                            i--;
//                                        }
//                                    }
//                                }
//                            }
//                            this.lastCompletedInstruction = executionObj.instructionID;
//                        }
//                    }
//
//
//                    if (executionObj.opcode == Opcode.HALT) {
//                        break;
//                    }
//                    //System.out.println("mem access finished one cycle");
//                    memAccessClockQueue.put("tick please!");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("memory access has broken out of its loop");
//            shutdown();
//        });
    }

    public void clock()
    {
        while(!this.finished)
        {
            if(!this.isStalled) {
                this.fetchedInstruction = fetch();
                this.decodedInstruction = decode(this.fetchedInstruction);
                this.readyInstructions.add(issue(this.decodedInstruction));
            }
            execute(this.readyInstructions.get(0));
            this.readyInstructions.remove(0);
            this.cycles++;
        }
    }

    public Instruction fetch() {
        System.out.println("fetching");

        Instruction instruction = this.instructions.get(this.PC);
        System.out.println(instruction.opcode);
         return instruction;

//        if (instruction.opcode == Opcode.HALT) {
//            this.finished = true;
//            System.out.println("set finished to true inside fetch");
//        }

    }

    public ExecutionObj decode(Instruction instruction) {
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
        return new ExecutionObj(instruction.opcode, constant, resultRegister, r1, r2,
                target_address, memory_address);

    }


    public ExecutionObj issue(ExecutionObj executionObj) {
        //TODO switch statement based on the opcode, can just merge with decode unit tbh.
        //TODO remove dependencies from list when they are executed.
        //TODO issuing logic.
        System.out.println("issue");

        boolean isAluInstruction = isAluInstruction(executionObj.opcode);
        return executionObj;
    }

    public void executionUnit(ExecutionState executionState)
    {
        if(executionState == null)
        {
            executionState = new ExecutionState(this.readyInstructions.get(0));
            this.readyInstructions.remove(0);
        }
        else {
            executionState = this.executionState;
        }
        ExecutionObj executionObj = executionState.executionObj;
        if (executionState.isComplete()) {
            //take a new object from the queue. (reservation station)
            this.executionState = new ExecutionState(this.readyInstructions.get(0));
            this.readyInstructions.remove(0);
            executionState = this.executionState;
            executionState.currentCycleNumber++;
            if (executionState.isComplete()) {
                execute(executionObj);
                this.isStalled = false;

            }
        } else {
            this.isStalled = true;
            this.executionState = executionState;
            executionState.currentCycleNumber++;
            if (executionState.isComplete()) {
                execute(executionObj);
                this.isStalled = false;
            }
        }
        if (executionObj.opcode == Opcode.HALT) {
            this.finished = true;
            System.out.println("set finished to true in execution unit");
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
        //TODO this should send a pair to set in memory to writeback
        this.instructionsCount++;
        switch (opcode) {// BR ADDRESS
            case BR: {
                newPC = target_address;
                break;
            }
            case BZ: {// BZ R1 ADDRESS
                if (this.registers.get(r1) == 0)
                    newPC = target_address;
                else
                    newPC = this.PC + 1;
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
                this.registers.set(resultRegister, (first + second));
                System.out.println("added registers :- " + r1 + " and " + r2 + " to get " + this.registers.get(resultRegister));
                newPC = this.PC + 1;
                break;
            }
//SUB R1 R2 REGRESULT
            case SUB: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first - second);
                newPC = this.PC + 1;
                break;
            }
//CMP R1 R2 REGRESULT
            case CMP: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                if (first > second) {
                    this.registers.set(resultRegister, 1);
                } else if (first == second) {
                    this.registers.set(resultRegister, 0);
                } else {
                    this.registers.set(resultRegister, -1);
                }
                newPC = this.PC + 1;
                break;
            }
//AND R1 R2 REGRESULT
            case AND: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                this.registers.set(resultRegister, first & second);
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
                this.registers.set(resultRegister, first * second);
                newPC = this.PC + 1;
                break;
            }
//NOT R1 REGRESULT
            case NOT: {
                int first = this.registers.get(r1);
                this.registers.set(resultRegister, ~first);
                newPC = this.PC + 1;
                break;
            }
//ADD R1 CONST REGRESULT
            case ADDI: {
                int value = this.registers.get(r1);
                this.registers.set(resultRegister, value + constant);
                newPC = this.PC + 1;
                break;
            }
//BLEQ R1 R2 ADDRESS
            case BLEQ: {
                int first = this.registers.get(r1);
                int second = this.registers.get(r2);
                if (first <= second) {
                    newPC = target_address;
                } else {
                    newPC = this.PC + 1;
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
                break;
            }
            default:
                System.out.println("Error, unknown OPCODE in execute");
                break;
        }
        this.PC = newPC;
    }

    public int alu(ExecutionObj executionObj) {
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;
        //process all arithmetic with integers in here. Some instructions may take several cycles
        switch (opcode) {
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
            default: {
                System.out.println("alu was wrongly assigned an instruction");
                return 1;
            }
        }

    }

    public int memoryAccess(ExecutionObj executionObj) {
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;
        switch (opcode) {
            case LD: {// LD R1 MEM_ADDRESS
                this.registers.set(r1, this.registers.get(memory_address));
                this.incrementPC();
                return 0;
            }
            case MOVA: {// MOVA R1 R2
                this.registers.set(resultRegister, this.registers.get(this.registers.get(r1)));
                this.incrementPC();
                return 0;
            }
            case MOVB: {// MOVB R1 R2
                this.registers.set(resultRegister, this.registers.get(r1));
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
            default:
                System.out.println("invalid execution object sent to memory access unit");
                return 1;
        }
    }

    public void branchPredictor(ExecutionObj executionObj) {
        //takes in a branch instruction as an input. needs to set a checkpoint that it can revert to if incorrect
        // if (target_address > this.PC) don't take branch else take branch for a static implementation. Can create
        // a cache of known branches to implement dynamic branch prediction at a later date.
        Opcode opcode = executionObj.opcode;
        int constant = executionObj.constant;
        int resultRegister = executionObj.resultRegister;
        int r1 = executionObj.r1;
        int r2 = executionObj.r2;
        int target_address = executionObj.target_address;
        int memory_address = executionObj.memory_address;

        int checkpointPC = this.PC;
        ArrayList<Integer> checkPointRegisters = this.registers;
        ArrayList<Integer> checkPointMem = this.memory;
        //always take backwards branches
        if (target_address > this.PC) {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, false);
            this.incrementPC();
        } else {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, true);
            this.PC = target_address;
        }
        //TODO actually calculate the value of the branch. I think that we can check every branch instruction against executionObj
        //and it's actual value. If this is different. reload the last checkpoint.


    }


    private boolean isAluInstruction(Opcode opcode) {
        System.out.println("entered into alu function");
        switch (opcode) {
            case DIV:
            case CMP:
            case MUL:
            case BZ:
            case BLEQ:
            case BR:
            case ADD:
            case SUB:
            case AND:
            case NOT:
            case ADDI:
            case MOV:
            case LDI:
            case MOVA:
            case MOVB: {
                return true;
            }
            case LD:
            case ST:
            case HALT: {
                return false;
            }
            default:
                System.out.println("Error, unknown OPCODE in issue assign");
                return false;
        }
    }


    public void writeBack() {
        for (int i = 0; i < registers.size(); i++) {
            if (!Objects.equals(registers.get(i), registerFile.get(i))) {
                registerFile.set(i, registers.get(i));
            }
        }

        //TODO receive messages from execute to set memory.
    }

    private void incrementPC() {
        this.PC = this.PC + 1;
    }


    private Dependency getDependencyByInstruction(ArrayList<Dependency> dependencyList, int i) {
        for (Dependency dependency : dependencyList) {
            if (dependency.instructionWaiting == i) {
                return dependency;
            }
        }
        // this should never happen
        return dependencyList.get(0);
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