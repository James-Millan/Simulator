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