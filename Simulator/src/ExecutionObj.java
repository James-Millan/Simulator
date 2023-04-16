public class ExecutionObj {
    public ExecutionObj(Opcode opcode, int constant, int resultRegister, int r1, int r2, int target_address,
                        int memory_address ) {
        this.opcode = opcode;
        this.constant = constant;
        this.resultRegister = resultRegister;
        this.r1 = r1;
        this.r2 = r2;
        this.target_address = target_address;
        this.memory_address = memory_address;

    }
    public Opcode opcode;
    public int constant;
    public int resultRegister;
    public int r1;
    public int r2;
    public int target_address;
    public int memory_address;
    public int instructionID;


    @Override
    public boolean equals(Object object) {
        ExecutionObj anotherObj= (ExecutionObj) object; //downcasting from object to Person
        if (!this.opcode.equals(anotherObj.opcode)) {
            return false;
        }
        else if(!(this.constant == anotherObj.constant))
        {
            return false;
        }
        else if(!(this.resultRegister == anotherObj.resultRegister))
        {
            return false;
        }
        else if(!(this.r1 == anotherObj.r1))
        {
            return false;
        }
        else if(!(this.r2 == anotherObj.r2))
        {
            return false;
        }
        else if(!(this.target_address == anotherObj.target_address))
        {
            return false;
        }
        else if(!(this.memory_address == anotherObj.memory_address))
        {
            return false;
        }
        return true;
    }
}
