import java.util.ArrayList;

public class BranchTargetBuffer {
    public ArrayList<BranchEntry> entries = new ArrayList<>();

    public BranchTargetBuffer()
    {

    }

    public BranchEntry get(int addr)
    {
        for (BranchEntry entry : entries) {
            if(entry.addr == addr)
            {
                return entry;
            }
        }
        return null;
    }
    public boolean contains(int addr)
    {
        for (BranchEntry entry : entries) {
            if(entry.addr == addr)
            {
                return true;
            }
        }
        return false;
    }
}
