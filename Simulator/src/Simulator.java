import java.io.File;
import java.util.*;
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
            arf.add(0);
            rat.add(new RATEntry(i,true,-1));
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
    public ArrayList<Integer> arf = new ArrayList<>();
    public ReservationStationObject[] reservationStation = new ReservationStationObject[16] ;
    public ArrayList<ExecutionObj> aluResStat = new ArrayList<>();
    public ArrayList<ExecutionObj> memAccessResStat = new ArrayList<>();
    public ArrayList<ExecutionObj> readyInstructions = new ArrayList<>();
    public ArrayList<Integer> registers = new ArrayList<>();
    public ArrayList<RATEntry> rat = new ArrayList<>();
    //public HashMap<Integer, Integer> rat = new HashMap<>();
    public ArrayList<Integer> memory = new ArrayList<>();
    public ArrayList<Instruction> instructions = new ArrayList<>();
    public ArrayList<ExecutionObj> reorderBuffer = new ArrayList<>();

    public Instruction fetchedInstruction = null;
    public ExecutionObj decodedInstruction = null;

    public ExecutionState executionState = null;
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

        clock();
    }

    public void clock() {
        while (!this.finished) {
            if (!this.isStalled) {
                this.fetchedInstruction = fetch();
                ExecutionObj decodedObj = decode(this.fetchedInstruction);
                this.decodedInstruction = decodedObj;
                reorderBuffer.add(decodedObj);

                issue(this.decodedInstruction);
            }
            executionUnit();
            //alu2();
            //memoryAccess();
            this.cycles++;
            if (this.cycles > 100000) {
                this.finished = true;
            }
        }
        this.arf = this.registers;
    }

    public Instruction fetch() {
        System.out.println("fetching");

        Instruction instruction = this.instructions.get(this.PC);
        System.out.println(instruction.opcode);
        if(isBranch(instruction.opcode))
        {
            //send to branch predictor.
            branchPredictor(decode(instruction));
        }
        return instruction;
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


    public void issue(ExecutionObj executionObj) {

        ReservationStationObject reservationStationObject = new ReservationStationObject(0,0,executionObj.opcode, true);
        System.out.println("issue");
        //TODO register renaming in RAT.
        if(!(executionObj.r1 == -1)) {
            if (!rat.get(executionObj.r1).done) {
                int provider = rat.get(executionObj.r1).provider;
                if (provider != -1) {
                    reservationStationObject.operand1Provider = provider;
                } else {
                    reservationStationObject.operand1 = this.registers.get(executionObj.r1);
                }
                reservationStationObject.ready = false;
            }
            else {
                reservationStationObject.operand1 = registers.get(executionObj.r1);
            }
        }
        if(!(executionObj.r2 == -1)) {
            if (!rat.get(executionObj.r2).done) {
                int provider = rat.get(executionObj.r2).provider;
                if (provider != -1) {
                    reservationStationObject.operand2Provider = provider;
                } else {
                    reservationStationObject.operand2 = this.registers.get(executionObj.r2);
                }

                reservationStationObject.ready = false;
            } else {
                reservationStationObject.operand2 = registers.get(executionObj.r2);
            }
        }
        for(int i = 0; i < reservationStation.length; i++)
        {
            if(reservationStation[i] == null)
            {
                reservationStation[i] = reservationStationObject;
                rat.get(executionObj.resultRegister).provider = i;
            }
        }
//        boolean isAluInstruction = isAluInstruction(executionObj.opcode);
//        if (isAluInstruction) {
//            this.aluResStat.add(executionObj);
//        } else {
//            this.memAccessResStat.add(executionObj);
//        }
    }

    public void executionUnit() {
        int id = -1;
        if (this.executionState == null) {
            ReservationStationObject reservationStationObject = null;

            for(int j = 0; j < reservationStation.length; j++)
            {
                if(reservationStation[j].ready)
                {
                    reservationStationObject = reservationStation[j];
                    id = j;
                }
            }
            if(reservationStationObject == null)
            {
                return;
            }
            this.executionState = new ExecutionState(reservationStationObject);
        }
        executionState.currentCycleNumber++;
        if (executionState.isComplete()) {
            int result = executeALU(executionState);
            //TODO broadcast result
            for(int i = 0; i < reservationStation.length; i++)
            {
                if(reservationStation[i].operand1Provider == id )
                {
                    reservationStation[i].operand1 = result;
                    reservationStation[i].operand1Provider = -1;
                }
                else if(reservationStation[i].operand2Provider == id )
                {
                    reservationStation[i].operand2 = result;
                    reservationStation[i].operand2Provider = -1;
                }
                if(reservationStation[i].operand2Provider == -1 && reservationStation[i].operand1Provider == -1)
                {
                    reservationStation[i].ready = true;
                }
            }
            //TODO writeback result.
            for(int j = 0; j < rat.size(); j++)
            {
                if(rat.get(j).provider == id)
                {
                    this.registers.set(j, result);
                    this.rat.get(j).provider = -1;
                }
            }
            reservationStation[id] = null;
            this.executionState = null;
            this.isStalled = false;
        } else {
            this.isStalled = true;
        }


    }


    public int executeALU(ExecutionState executionState)
    {
        System.out.println("entered new execute function");
        int op1 = executionState.operand1;
        int op2 = executionState.operand2;
        switch (executionState.opcode)
        {
            case DIV:
                return (int)Math.floor(op1 / op2);
            case MUL:
                return op1 * op2;
            case ADD: return op1 + op2;
            case ADDI: return op1 + op2;
            case SUB: return op1 - op2;
            case HALT: this.finished = true;
                return 0;
        }
        return 0;
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
                    if(sameAsLastCheckpoint && !this.lastCheckpoint.branchTaken)
                    {
                        //TODO restore from checkpoint and flush pipeline.
                        this.registers = this.lastCheckpoint.registers;
                        this.memory = this.lastCheckpoint.memory;
                        this.fetchedInstruction = null;
                        this.decodedInstruction = null;
                        this.aluState = null;
                        this.memAccessState = null;
                        this.aluResStat.clear();
                        this.memAccessResStat.clear();
                    }
                    newPC = target_address;
                } else {
                    if (sameAsLastCheckpoint && this.lastCheckpoint.branchTaken) {
                        //TODO restore from checkpoint and flush pipeline.
                        flush();
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
                int result = first / second;
                this.registers.set(resultRegister, result);
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
                System.out.println("alu queue empty");
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
                System.out.println("alu queue empty");
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
                System.out.println("memory access queue is empty");
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
        //always take backwards branches
        if (target_address > this.PC) {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, false);
            this.incrementPC();
        } else {
            this.lastCheckpoint = new Checkpoint(checkpointPC, checkPointRegisters, checkPointMem, executionObj, true);
            this.PC = target_address;
        }
    }

    private void flush()
    {
        this.registers = this.lastCheckpoint.registers;
        this.memory = this.lastCheckpoint.memory;
        this.fetchedInstruction = null;
        this.decodedInstruction = null;
        this.aluState = null;
        this.memAccessState = null;
        this.aluResStat.clear();
        this.memAccessResStat.clear();
    }

    private void commit()
    {
        this.arf = this.registers;
    }
    public boolean isAvailable(ExecutionObj obj)
    {
        int r1 = obj.r1;
        int r2 = obj.r2;
        int resultReg = obj.resultRegister;
        boolean isAvailable = true;
        if(r1 != -1 && !rat.get(r1).done)
        {
            isAvailable = false;
        } else if (r2 != -1 && !rat.get(r2).done) {
            isAvailable = false;
        }
        else if (resultReg != -1 && !rat.get(resultReg).done)
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