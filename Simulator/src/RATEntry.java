public class RATEntry {
    public int register;
    public boolean done;
    public int provider;

    RATEntry(int reg, boolean done, int prov)
    {
        this.register = reg;
        this.done = done;
        //provider is -1 if we can just use register file.
        this.provider = prov;
    }
}
