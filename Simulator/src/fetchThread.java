import java.util.concurrent.BlockingQueue;

public class fetchThread implements Runnable{
    public Simulator simulator;
    BlockingQueue<String> fetchQueue;
    BlockingQueue<Instruction> decodeQueue;
    BlockingQueue<String> fetchClockQueue;
    public fetchThread(Simulator sim, BlockingQueue<String> fetchQueue,BlockingQueue<Instruction> decodeQueue, BlockingQueue<String> fetchClockQueue)
    {
        this.simulator = sim;
        this.fetchQueue = fetchQueue;
        this.decodeQueue = decodeQueue;
        this.fetchClockQueue = fetchClockQueue;
    }

    @Override
    public void run() {
        while (!simulator.finished) {
            try {
                String message = fetchQueue.take();
                System.out.println("Fetching... " + message);
                if (simulator.PC >= 0 && simulator.PC < simulator.instructions.size())
                {
                    Instruction instruction = simulator.instructions.get(simulator.PC);
                    decodeQueue.put(instruction);
                }
                else {
                    simulator.finished = true;
                }
                fetchClockQueue.put("tick please!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
