public class BranchEntry {
    public int addr;
    public int taken;
    public int target;

    public BranchEntry(int addr, int taken, int target)
    {
        this.addr = addr;
        this.taken = taken;
        this.target = target;
    }
}
