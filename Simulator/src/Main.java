public class Main {
    public static void main(String[] args) {
       Simulator simulator = new Simulator();
       try
       {
           simulator.initialise();
       }
       catch(Exception e)
       {
           System.out.println(e);
       }
       System.out.println("number of cycles:- " + simulator.cycles);
       for(int i = 0; i < simulator.registers.size(); i++)
       {
           System.out.println("register " + i + " :- "  + simulator.registers.get(i));
       }
    }


}
