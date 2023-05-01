import java.util.ArrayList;

public class Dependency {
    public int instructionWaiting;
    public ArrayList<Integer> dependencies;

    public Dependency(int instructionWaiting, ArrayList<Integer> dependencies)
    {
        this.instructionWaiting = instructionWaiting;
        this.dependencies = dependencies;
    }

}
