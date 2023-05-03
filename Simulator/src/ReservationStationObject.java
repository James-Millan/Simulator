public class ReservationStationObject {
    public int operand1;
    public int operand2;
    public int operand1Provider;
    public int operand2Provider;
    public Opcode opcode;
    public boolean ready;
    ReservationStationObject(int op1, int op2, Opcode opcode, boolean ready)
    {
        this.operand1 = op1;
        this.operand2 = op2;
        this.opcode = opcode;
        this.ready = ready;
        this.operand1Provider = -1;
        this.operand2Provider = -1;
    }
}
