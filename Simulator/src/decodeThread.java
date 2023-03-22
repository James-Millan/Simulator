import java.util.concurrent.BlockingQueue;

public class decodeThread implements Runnable{
    public Simulator simulator;
    BlockingQueue<Instruction> decodeQueue;
    BlockingQueue<ExecutionObj> executionQueue;
    BlockingQueue<String> decodeClockQueue;
    public decodeThread(Simulator sim, BlockingQueue<Instruction> decodeQueue,BlockingQueue<ExecutionObj> executionQueue, BlockingQueue<String> decodeClockQueue)
    {
        this.simulator = sim;
        this.decodeQueue = decodeQueue;
        this.executionQueue = executionQueue;
        this.decodeClockQueue = decodeClockQueue;
    }

    @Override
    public void run() {
        while (!simulator.finished) {
            try {
                Instruction instruction = decodeQueue.take();
                ExecutionObj executionObj = simulator.decode(instruction);
                System.out.println("Decode received message: ");
                executionQueue.put(executionObj);
                decodeClockQueue.put("tick please!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
