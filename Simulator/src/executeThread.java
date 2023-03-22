import java.util.concurrent.BlockingQueue;

public class executeThread implements Runnable{
    public Simulator simulator;
    BlockingQueue<Instruction> decodeQueue;
    BlockingQueue<ExecutionObj> executionQueue;
    BlockingQueue<String> executeClockQueue;
    public executeThread(Simulator sim, BlockingQueue<ExecutionObj> executionQueue, BlockingQueue<String> executeClockQueue)
    {
        this.simulator = sim;
        this.executionQueue = executionQueue;
        this.executeClockQueue = executeClockQueue;
    }

    @Override
    public void run() {
        while (!simulator.finished) {
            try {
                ExecutionObj executionObj = executionQueue.take();
                System.out.println("Execution received message");
                simulator.execute(executionObj);
                executeClockQueue.put("tick please!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
