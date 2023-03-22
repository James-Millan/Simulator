import java.util.ArrayList;

public class Checkpoint {
    public int PC;
    public ArrayList<Integer> registers = new ArrayList<>();
    public ArrayList<Integer> memory = new ArrayList<>();
    public ExecutionObj executionObj;
    public Boolean branchTaken;

    public Checkpoint(int pc, ArrayList<Integer> registers, ArrayList<Integer> memory, ExecutionObj executionObj, Boolean branchTaken)
    {
        this.PC = pc;
        this.registers = registers;
        this.memory = memory;
        this.executionObj = executionObj;
        this.branchTaken = branchTaken;
    }
}
