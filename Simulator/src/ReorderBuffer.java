import java.util.ArrayList;

public class ReorderBuffer {
    public int commit;
    public int issue;
    public ArrayList<ROBEntry> entries;

    ReorderBuffer(int commit, int issue, ArrayList<ROBEntry> entries)
    {
        this.commit = commit;
        this.issue = issue;
        this.entries = entries;
    }
    public void incrementCommit()
    {
        this.commit++;
        this.commit = this.commit % entries.size();
    }
    public void incrementIssue()
    {
        this.issue++;
        this.issue = this.issue % entries.size();
    }
}

class ROBEntry {
    public int register;
    public int value;
    public boolean done;

    ROBEntry(int register, int value, boolean done)
    {
        this.register = register;
        this.value = value;
        this.done = done;
    }

}
