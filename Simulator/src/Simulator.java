import java.util.ArrayList;

enum Opcode {
    ADD,
    ADDI,
    MUL,
    CMP,
    LD,
    LDI,
    MOV,
    ST,
    BR,
    BZ,
    JU,
    BLEQ,
    AND,
    NOT,
    HALT
}

public class Simulator {
//    public static void main(String[] args) {
//        System.out.println("Hello world!");
//    }
    public Simulator(){

    }
    public boolean finished = false;
    public int PC = 0;
    public int cycles = 0;
    public int numOfInstructions = 0;
    public ArrayList<Instruction> instructions;
    public Instruction fetch(){
        return this.instructions.get(this.PC);
    }
    public void decode(){
        System.out.println("decode");
    }

    public int execute(Opcode opcode,int con, int r1, int r2, int target_address){
        switch(opcode){
            case BR:
                this.PC = target_address;
                return 0;
            case BZ:
                return 0;
            case JU: return 0;
            case LD: return 0;
            case ST: return 0;
            case ADD: return 0;
            case CMP: return 0;
            case AND: return 0;
            case LDI: return 0;
            case MOV: return 0;
            case MUL: return 0;
            case NOT: return 0;
            case ADDI: return 0;
            case BLEQ: return 0;
            case HALT:
                this.finished = true;
                return 0;
            default:
                System.out.println("Error, unknown OPCODE");
                return 1;
        }
    }
    private void incrementPC() {
        this.PC = this.PC + 1;
    }
}


/* Defining ISA

add - 0000 adds two operands into a destination register.
addi - 0001
cmp - 0010
ld - 0011
ldi - 0100
br - 0101
ju - 0110
bleq - 0111
and - 1000
not - 1001
mov - 1010
mul - 1011
bz - 1100
 */