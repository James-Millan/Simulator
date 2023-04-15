public class ExecutionState {
    public ExecutionObj executionObj;
    public int cyclesForInstruction;
    public int currentCycleNumber;
    public ExecutionState(ExecutionObj executionObj)
    {
        this.executionObj = executionObj;
        this.cyclesForInstruction = getNumCycles(executionObj);
        this.currentCycleNumber = 0;
    }
    public boolean isComplete(){
        return currentCycleNumber == cyclesForInstruction;
    }

    public int getNumCycles(ExecutionObj executionObj) {
        Opcode opcode = executionObj.opcode;
        switch(opcode){
            case BR:
            case ADD:
            case SUB:
            case AND:
            case NOT:
            case ADDI:
            case MOV:
            case LDI:
            case HALT: {
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
                System.out.println("Error, unknown OPCODE");
                return 1;
        }
    }
}
