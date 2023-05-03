public class ExecutionState {
    public ExecutionObj executionObj;
    public int cyclesForInstruction;
    public int currentCycleNumber;
    public int operand1;
    public int operand2;
    public int result;
    public Opcode opcode;
    public ExecutionState(ExecutionObj executionObj)
    {
        this.executionObj = executionObj;
        this.cyclesForInstruction = getNumCycles(executionObj.opcode);
        this.currentCycleNumber = 0;
    }

    public ExecutionState(ReservationStationObject obj)
    {
        this.opcode = obj.opcode;
        this.operand1 = obj.operand1;
        this.operand2 = obj.operand2;
        this.result = 0;
        this.cyclesForInstruction = getNumCycles(opcode);
        this.currentCycleNumber = 0;
    }
    public boolean isComplete(){
        return currentCycleNumber == cyclesForInstruction;
    }

    public int getNumCycles(Opcode opcode) {
        switch(opcode){
            case BR:
            case ADD:
            case SUB:
            case AND:
            case NOT:
            case ADDI:
            case MOV:
            case LDI:
            case HALT:
            case MOVB:
            case MOVA:{
                return 1;
            }
            case BZ:
            case BLEQ: {
                return 2;
            }
            case LD:
            case ST: {
                return 4;
            }
            case CMP:
            case MUL: {
                return 3;
            }
            case DIV:
                return 10;
            default:
                System.out.println("Error, unknown OPCODE in execution state");
                System.out.println(opcode);
                return 1;
        }
    }
}