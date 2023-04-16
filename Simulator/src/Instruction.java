public class Instruction {
    public int linenumber;
    public Opcode opcode;
    public String[] operands;

    public Instruction(int linenumber, String opcode, String[] operands) {
        this.linenumber = linenumber;
        this.operands = operands;
        this.opcode = getOpcode(opcode);
    }

    private Opcode getOpcode(String opcode)
    {
        Opcode actualOpcode = Opcode.HALT;
        switch(opcode) {
            case "BR": { // BR ADDRESS
                actualOpcode = Opcode.BR;
                break;
            }
            case "BZ": {// BZ R1 ADDRESS
                actualOpcode = Opcode.BZ;
                break;
            }
            case "LD": {// LD R1 MEM_ADDRESS
                actualOpcode = Opcode.LD;
                break;
            }
            case "ST": {// ST R1 R2
                actualOpcode = Opcode.ST;
                break;
            }
            case "ADD": {// ADD R1 R2 REGRESULT
                actualOpcode = Opcode.ADD;
                break;
            }
            case "SUB": {//SUB R1 R2 REGRESULT
                actualOpcode = Opcode.SUB;
                break;
            }
            case "CMP": {//CMP R1 R2 REGRESULT
                actualOpcode = Opcode.CMP;
                break;
            }
            case "AND": {//AND R1 R2 REGRESULT
                actualOpcode = Opcode.AND;
                break;
            }
            case "LDI": {//LDI REGRESULT CONST
                actualOpcode = Opcode.LDI;
                break;
            }
            case "MOV": {//MOV R1 REGRESULT
                actualOpcode = Opcode.MOV;
                break;
            }
            case "MUL": {//MUL R1 R2 REGRESULT
                actualOpcode = Opcode.MUL;
                break;
            }
            case "NOT": {//NOT R1 REGRESULT
                actualOpcode = Opcode.NOT;
                break;
            }
            case "ADDI": {//ADD R1 CONST REGRESULT
                actualOpcode = Opcode.ADDI;
                break;
            }
            case "BLEQ": {//BLEQ R1 R2 ADDRESS
                actualOpcode = Opcode.BLEQ;
                break;
            }
            case "HALT": {// HALT
                actualOpcode = Opcode.HALT;
                break;
            }
            case "MOVA": {
                actualOpcode = Opcode.MOVA;
                break;
            }
            case "MOVB": {
                actualOpcode = Opcode.MOVB;
            }
            default:
                System.out.println("Error, unknown OPCODE when parsing input file");
                System.out.println(opcode);
        }
        return actualOpcode;
    }

}
