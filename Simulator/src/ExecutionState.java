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
        return currentCycleNumber >= cyclesForInstruction;
    }

    public int getNumCycles(ExecutionObj executionObj) {
        Opcode opcode = executionObj.opcode;
        switch(opcode){
            case BR:
            case ST:
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
                return 1;
            }
            case LD: {
                return 1;
            }
            case CMP:
            case MUL: {
                return 1;
            }
            case DIV:
                return 1;
            default:
                System.out.println("Error, unknown OPCODE");
                return 1;
        }
    }
}
