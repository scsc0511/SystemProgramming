import java.util.ArrayList;



/**
 * ����ڰ� �ۼ��� ���α׷� �ڵ带 �ܾ�� ���� �� ��, �ǹ̸� �м��ϰ�, ���� �ڵ�� ��ȯ�ϴ� ������ �Ѱ��ϴ� Ŭ�����̴�. <br>
 * pass2���� object code�� ��ȯ�ϴ� ������ ȥ�� �ذ��� �� ���� symbolTable�� instTable�� ������ �ʿ��ϹǷ� �̸� ��ũ��Ų��.<br>
 * section ���� �ν��Ͻ��� �ϳ��� �Ҵ�ȴ�.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit ������ �������� ���� ���� */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	boolean ltrgFlag = false;
	
	enum Register{A,X,L,B,S,T,F,PAD,PC,SW};
	
	
	/* Token�� �ٷ� �� �ʿ��� ���̺���� ��ũ��Ų��. */
	SymbolTable symTab;
	LiteralTable literalTab;
	InstTable instTab;
	ExtdefTable extDefTab;
	ArrayList<String> extRefTab;
	//�߰�
	int len;		//��ū�� ���� 
	ArrayList<String> modifRecordList;
	boolean litFlag;
	
	/** �� line�� �ǹ̺��� �����ϰ� �м��ϴ� ����. */
	ArrayList<Token> tokenList;
	
	/**
	 * �ʱ�ȭ�ϸ鼭 symTable�� instTable�� ��ũ��Ų��.
	 * @param symTab : �ش� section�� ����Ǿ��ִ� symbol table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		//...
		this.symTab = symTab;
		this.instTab = instTab;
		tokenList = new ArrayList<Token>();
		modifRecordList = new ArrayList<String>();
		this.len = 0;
		ltrgFlag = false;
		litFlag = false;
	}

	/**
	 * �ʱ�ȭ�ϸ鼭 literalTable�� instTable�� ��ũ��Ų��.
	 * @param literalTab : �ش� section�� ����Ǿ��ִ� literal table
	 * @param instTab : instruction ���� ���ǵ� instTable
	 */
	public TokenTable(LiteralTable literalTab, InstTable instTab) {
		//...
		this.literalTab = literalTab;
		this.instTab = instTab;
		tokenList = new ArrayList<Token>();
		modifRecordList = new ArrayList<String>();
		this.len = 0;
		ltrgFlag = false;
		litFlag = false;
	}
	
	/**
	 * �Ϲ� ���ڿ��� �޾Ƽ� Token������ �и����� tokenList�� �߰��Ѵ�.
	 * @param line : �и����� ���� �Ϲ� ���ڿ�
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line));
		this.len++;
	}
	
	/**
	 * tokenList���� index�� �ش��ϴ� Token�� �����Ѵ�.
	 * @param index
	 * @return : index��ȣ�� �ش��ϴ� �ڵ带 �м��� Token Ŭ����
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	public void removeToken(int index) {
		tokenList.remove(index);
		this.len--;
	}
	
	public int getLastId()
	{
		int lastId = tokenList.size() - 1;
		
		return lastId;
	}
	
	public int getOpcode(int id)
	{
		Instruction inst = instTab.search(tokenList.get(id).operator);
		if(inst == null)	return -1;
		int opCode = inst.opcode;
		
		return opCode;
	}
	
	public int getFormat(Token curTok)
	{
		if(curTok.operator.startsWith("+"))
			return 4;

		Instruction inst = instTab.search(curTok.operator);
		
		return inst.format;
	}
	
	
	public int getOpcode(String operator)
	{
		Instruction inst;
		
		if(operator.startsWith("+"))
			operator = operator.substring(1);
		
		inst = instTab.search(operator);
		if(inst == null)	return -1;
		int opCode = inst.opcode;
		
		return opCode;
	}
	
	public void setLiteralTable(LiteralTable literalTab)
	{
		this.literalTab = literalTab;
	}
	
	public void setExtDefTable(ExtdefTable extDefTab)
	{
		this.extDefTab = extDefTab;
	}
	
	public void setExtRefTable(ArrayList<String> extRefTab)
	{
		this.extRefTab = extRefTab;
	}
	
	private boolean isNumeric(String operand)
	{
		for(int i=0;i<operand.length();i++)
		{
			if(operand.charAt(i)<'0' || operand.charAt(i)>'9')
				return false;
		}
		
		return true;
	}
	
	private  byte[] intToByteArray(int value) 
	{
			byte[] byteArray = new byte[4];
			byteArray[0] = (byte)(value >> 24);
			byteArray[1] = (byte)(value >> 16);
			byteArray[2] = (byte)(value >> 8);
			byteArray[3] = (byte)(value);
			return byteArray;
	}
	
/*public*/	private  int byteArrayToInt(byte bytes[]) 
	{
		return ((((int)bytes[0] & 0xff) << 24) |
			(((int)bytes[1] & 0xff) << 16) |
			(((int)bytes[2] & 0xff) << 8) |
			(((int)bytes[3] & 0xff)));
	} 
	
	private int getRegisterNum(String reg)
	{
		if(reg == null)	return -1;
		Register curReg = Register.valueOf(reg);
		
		return curReg.ordinal();
	}
	
	
	
	private byte[] getValueOfOperand(String operand)
	{
		String value;
		int parseVal;
		int location;
		
		if(operand.startsWith("C'"))
		{
			value = operand.substring(2,operand.length()-1);
			return value.getBytes();
		}
		else if(operand.startsWith("X'"))
		{
			value = operand.substring(2,operand.length()-1);
			parseVal = Integer.parseInt(value,16);
			return intToByteArray(parseVal);
		}
		else if(operand.startsWith("="))
		{
			location = literalTab.search(operand);
			return intToByteArray(location);
		}
		else if(isNumeric(operand)||isNumeric(operand.substring(1)))
		{
			value = operand; 
			if(operand.startsWith("#"))
				value = operand.substring(1);
			parseVal = Integer.parseInt(value);
			return intToByteArray(parseVal);
		}
		else
		{
			String refSym;
			location = symTab.search(operand);
			if(location < 0)
			{
				for(int i=0;i<extRefTab.size();i++)
				{
					refSym = extRefTab.get(i);
					if(refSym.contentEquals(operand))
					{
						location = -1;
						break;
					}
				}
			}
				
			return intToByteArray(location);
		}		
	}
	private int calculateExpression(String operand)
	{
		
		if(operand == null)	return 0;
		
		String[] strVals;
		byte[] byteVal;
		int intVal1 = 0, intVal2 = 0;
		boolean pFlag = true;

		
		if(operand.contains("+"))
			strVals = operand.split("+");
		else if(operand.contains("-"))
		{
			strVals = operand.split("-");
			pFlag = false;
		}
		else
			return -1;
		byteVal = getValueOfOperand(strVals[0]);
		intVal1 = byteArrayToInt(byteVal);
		byteVal = getValueOfOperand(strVals[1]);
		intVal2 = byteArrayToInt(byteVal);
		if(intVal1 < 0 || intVal2 < 0)
			return -2;
		if(pFlag)
			intVal1 += intVal2;
		else
			intVal1 -= intVal2;
		
		return intVal1;
	}
	
	private String setFlagtoMakeObCode(Token curTok)
	{
		
		String strOpr;
		int opcode;
		
		if(curTok.operand[0] != null && curTok.operand[0].charAt(0) == '#')
		{
			curTok.setFlag(nFlag|bFlag|pFlag|eFlag, 0);
			curTok.setFlag(iFlag, 1);
			strOpr = curTok.operand[0].substring(1);
		}
		else if(curTok.operand[0] != null && curTok.operand[0].charAt(0) == '@')
		{
			curTok.setFlag(nFlag, 1);
			curTok.setFlag(iFlag, 0);
			strOpr = curTok.operand[0].substring(1);
		}	
		else
		{
			curTok.setFlag(nFlag, 1);
			curTok.setFlag(iFlag, 1);
			strOpr = curTok.operand[0];
		}
		
		if(curTok.operand[1] != null && curTok.operand[1].contentEquals("X"))
			curTok.setFlag(xFlag, 1);
		
		opcode = Integer.parseInt(curTok.objectCode,16);
		opcode = opcode + (curTok.nixbpe/iFlag);
		curTok.objectCode = String.format("%02X", opcode);
	
		return strOpr;
	}
	
	private void makeInstObjectCode(Token curTok)
	{
		Instruction inst = instTab.search(curTok.operator);
		int intOpr;
		String strOpr = null;
		String hexOpcode;
		
		hexOpcode = String.format("%02X", inst.opcode);
		curTok.objectCode = hexOpcode;;
		
		
		
		switch(getFormat(curTok))
		{
		case 1:
			break;
		case 2:
			if(curTok.equals("SVC"))
			{
				intOpr = byteArrayToInt(getValueOfOperand(curTok.operand[0]));
				strOpr = String.format("%1X",intOpr);
				curTok.objectCode = curTok.objectCode  + strOpr;
				break;
			}
			intOpr = getRegisterNum(curTok.operand[0]);
			strOpr = String.format("%1X", intOpr);
			curTok.objectCode = curTok.objectCode + strOpr;
			
			if(curTok.operator.equals("SHIFTL") 
			|| curTok.operator.equals("SHIFTR"))
			{
				intOpr = byteArrayToInt(getValueOfOperand(curTok.operand[1]));
				strOpr = String.format("%1X",intOpr);
				curTok.objectCode = curTok.objectCode  + strOpr;
				break;
			}
			intOpr = getRegisterNum(curTok.operand[1]);
			if(intOpr < 0)	intOpr = 0;
			strOpr = String.format("%1X", intOpr);
			curTok.objectCode = curTok.objectCode + strOpr;
			break;
		case 3:
			strOpr = setFlagtoMakeObCode(curTok);
			intOpr = calculateExpression(strOpr);
			if(intOpr < 0)
				intOpr = byteArrayToInt(getValueOfOperand(strOpr));
			

			//Displacement ��� 
			if(curTok.getFlag(pFlag)>0)
			{
				intOpr = intOpr - curTok.location - curTok.byteSize;
				if(intOpr < 0)
					intOpr = (intOpr & (16*16*16 -1));
			}
			curTok.objectCode = curTok.objectCode +String.format("%01X",(curTok.nixbpe%iFlag))+String.format("%03X",intOpr);
			break;
		case 4:
		    strOpr = setFlagtoMakeObCode(curTok);
			intOpr = calculateExpression(strOpr);
			if(intOpr < 0)
				intOpr = byteArrayToInt(getValueOfOperand(strOpr));
			if(intOpr < 0)
				intOpr = 0;
			//displacement
			curTok.objectCode = curTok.objectCode+String.format("%01X",(curTok.nixbpe%iFlag)) + String.format("%05X",intOpr);
			break;
		}
	
	}
	
	private void makeDirectiveObjectCode(Token curTok)
	{
		Assembler.Directives curDirc = Assembler.Directives.valueOf(curTok.operator);
		String strOpr;
		String lit;
		int intOpr;
		byte[] bytesOpr;
		boolean zeroFlag = true;
		
		switch(curDirc) {
		case START:
			curTok.objectCode ="H"+String.format("%-6s", curTok.label)+String.format("%06X",curTok.location);
			break;
		case END:
			curTok.objectCode = "E";
			break;
		case BYTE:
			strOpr = curTok.operand[0];
			
			bytesOpr = getValueOfOperand(strOpr);
			if(!strOpr.startsWith("C"))
			{
				intOpr = byteArrayToInt(bytesOpr);
				strOpr = "";
				for(int i=0;i<4;i++)
				{
					if(zeroFlag && bytesOpr[i] == 0)	continue;
					zeroFlag = false;
					strOpr += String.format("%02X", bytesOpr[i]);
				}
			}
			else
			{
				strOpr = "";
				for(int i=0;i<bytesOpr.length;i++)
				{
					strOpr += String.format("%02X", bytesOpr[i]);
				}
			}
			
			curTok.objectCode = new String(strOpr);
			break;
		case WORD:
			intOpr = calculateExpression(curTok.operand[0]);
			if(intOpr == -1)
				intOpr = byteArrayToInt(getValueOfOperand(curTok.operand[0]));
			else if(intOpr == -2)
				intOpr = 0;
			curTok.objectCode = String.format("%06X", intOpr);
			break;
		case RESB:
			break;
		case RESW:
			break;
		case CSECT:
			curTok.objectCode ="H"+String.format("%-6s", curTok.label)+String.format("%06X",curTok.location);
			break;
		case EXTDEF:
			String sym;
			int location;
			String strLoc;
			curTok.objectCode = "D";
			for(int i=1;i<extDefTab.len;i++)
			{
				sym =extDefTab.getSymbol(i);
				location = extDefTab.getAddr(sym);
				sym = String.format("%-6s", sym);
				strLoc = String.format("%06X", location);
				curTok.objectCode += (sym + strLoc);
			}
			break;
		case EXTREF:
			curTok.objectCode = new String("R");
			for(int i=0;i<extRefTab.size();i++)
			{
				sym = extRefTab.get(i);
				curTok.objectCode += String.format("%-6s", sym);
			}
			break;
		case EQU:
			break;
		case ORG:
			break;
		case LTORG:
			curTok.objectCode = new String("");
			for(int i=0;i<literalTab.len;i++)
			{
				lit = literalTab.getLiteral(i);
				strOpr = lit.substring(1);
				bytesOpr = getValueOfOperand(strOpr);
				//X�� ���
				if(strOpr.startsWith("X"))
				{
					intOpr = byteArrayToInt(bytesOpr);
					strOpr = "";
					for(int j=0;j<4;j++)
					{
						if(zeroFlag && bytesOpr[j] == 0)	continue;
						zeroFlag = false;
						strOpr += String.format("%02X", bytesOpr[j]);
					}
				}
				//C�� ���
				else if(strOpr.startsWith("C"))
				{
					strOpr = "";
					for(int j=0;j<bytesOpr.length;j++)
					{
						strOpr += String.format("%02X", bytesOpr[j]);
					}
				}
				else 
				{
					intOpr = byteArrayToInt(bytesOpr);
					strOpr = "";
					for(int j=0;j<6-bytesOpr.length;j++)
						strOpr += String.format("0");
					for(int j=0;j<bytesOpr.length;j++)
					{
						strOpr += String.format("%01X", bytesOpr[j]);
					}
				}
				curTok.objectCode = curTok.objectCode + strOpr;
				
			}
			break;
		}
	}
	
	private String processModifRecord(String operand, int stLoc, Instruction inst, char flag)
	{
		int hbSize = 0;
		String strStLoc;
		String strHbSize;
		String modifRecord = null;;
		
		
		if(inst == null)	//directive�� ���
			hbSize = 6;
		else				//����� ���(4����) 
		{
			stLoc += 1;
			hbSize = 5;
		}
		strStLoc = String.format("%06X", stLoc);
		strHbSize = String.format("%02X", hbSize);
		modifRecord = new String("M"+strStLoc+strHbSize+flag+operand);
		

		
		return modifRecord;
	}
	
	
	private void makeModifRecord(Token curTok)
	{
		if(curTok.operand[0] == null)	return;
		if(curTok.operator.contentEquals("EXTREF"))	return;
		
		String curRefSym;
		String modifRecord;
		String[] operands;
		char expFlag = '+';
		Instruction curInst = instTab.search(curTok.operator);
		
		
		if(curTok.operand[0].contains("+"))
		{
			operands = curTok.operand[0].split("+");
			for(int i=0;i<extRefTab.size();i++)
			{
				curRefSym = extRefTab.get(i);
				if(operands[0].contentEquals(curRefSym) || operands[1].contentEquals(curRefSym))
				{
					modifRecord = processModifRecord(operands[0],curTok.location,curInst,'+');
					modifRecordList.add(modifRecord);
					modifRecord = processModifRecord(operands[1],curTok.location,curInst,'+');
					modifRecordList.add(modifRecord);
					break;
				}
			}
			
		
		}
		else if(curTok.operand[0].contains("-"))
		{
			operands = curTok.operand[0].split("-");
			for(int i=0;i<extRefTab.size();i++)
			{
				curRefSym = extRefTab.get(i);
				if(operands[0].contentEquals(curRefSym) || operands[1].contentEquals(curRefSym))
				{
					modifRecord = processModifRecord(operands[0],curTok.location,curInst,'+');
					modifRecordList.add(modifRecord);
					modifRecord = processModifRecord(operands[1],curTok.location,curInst,'-');
					modifRecordList.add(modifRecord);
					break;
				}
			}
		}
		else
		{
			for(int i=0;i<extRefTab.size();i++)
			{
				curRefSym = extRefTab.get(i);
				if(curTok.operand[0].contentEquals(curRefSym))
				{
					modifRecord = processModifRecord(curTok.operand[0],curTok.location,curInst,'+');
					modifRecordList.add(modifRecord);
					break;
				}
		
			}
		}	
	}
	
	
	/**
	 * Pass2 �������� ����Ѵ�.
	 * instruction table, symbol table literal table ���� �����Ͽ� objectcode�� �����ϰ�, �̸� �����Ѵ�.
	 * @param index
	 */
	public void makeObjectCode(int index){
		//...
		Token curTok = tokenList.get(index);
		int opCode = getOpcode(index);
		
		if(opCode<0)	//directive
		{
			makeDirectiveObjectCode(curTok);
		}
		else			//���
		{
			makeInstObjectCode(curTok);
		}
		makeModifRecord(curTok);
	}
	
	/** 
	 * index��ȣ�� �ش��ϴ� object code�� �����Ѵ�.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		
		
		
		return tokenList.get(index).objectCode;
	}
	
	
	
}

/**
 * �� ���κ��� ����� �ڵ带 �ܾ� ������ ������ ��  �ǹ̸� �ؼ��ϴ� ���� ���Ǵ� ������ ������ �����Ѵ�. 
 * �ǹ� �ؼ��� ������ pass2���� object code�� �����Ǿ��� ���� ����Ʈ �ڵ� ���� �����Ѵ�.
 */
class Token{
	//�ǹ� �м� �ܰ迡�� ���Ǵ� ������
	int location;
	String label;
	String operator;
	String[] operand;
	String comment;
	char nixbpe;

	// object code ���� �ܰ迡�� ���Ǵ� ������ 
	String objectCode;
	int byteSize;
	

	/**
	 * Ŭ������ �ʱ�ȭ �ϸ鼭 �ٷ� line�� �ǹ� �м��� �����Ѵ�. 
	 * @param line ��������� ����� ���α׷� �ڵ�
	 */
	public Token(String line) {
		//initialize �߰�
		location = 0;
		label = null;operator=null;
		operand=new String[TokenTable.MAX_OPERAND];
		for(int i=0;i<operand.length;i++)
			operand[i] = null;
		comment=null;
		nixbpe = 0;
		objectCode = "";
		byteSize = 0;
		
		parsing(line);
	}
	
	/**
	 * line�� �������� �м��� �����ϴ� �Լ�. Token�� �� ������ �м��� ����� �����Ѵ�.
	 * @param line ��������� ����� ���α׷� �ڵ�.
	 */
	public void parsing(String line) {
		
		if(line.startsWith("."))	return;
		
		String[] tokens = line.split("	");
		String[] operands = null;
		
		if( tokens[0] != null)
			label = new String(tokens[0]);
		if(tokens[1] != null)	
			operator = new String(tokens[1]);
		
		if(tokens.length>=3)
		{
			
			
			if((tokens[2] != null) && (tokens[2].length() != 0))
				operands = tokens[2].split(",");
			if(operands != null)
			{
				for(int i=0,j=0;i<operands.length && operands[i] != null;i++,j++)
				{	
					this.operand[j] = new String(operands[i]);
				}
			}
			else if(tokens[2].length() != 0)
				this.operand[0] = new String(tokens[2]);
		}

		setNixbpe();
		if(tokens.length>=4 && tokens[3] != null)
			comment = new String(tokens[3]);
	}
	
	
	public void setNixbpe()
	{
		setFlag(TokenTable.nFlag|TokenTable.iFlag,1);
		
		if(operand[0] == null)	return;
	
		char sign = operand[0].charAt(0);
	

		//n,i Flag Setting
		if(sign == '#')
		{
			setFlag(TokenTable.iFlag,1);
			setFlag(TokenTable.nFlag,0);
		}
		else if(sign == '@')
		{
			setFlag(TokenTable.nFlag,1);
			setFlag(TokenTable.iFlag,0);
		}
		else
		{
			setFlag(TokenTable.nFlag | TokenTable.iFlag,1);
		}
		
		//X Flag Setting
		for(int i=0; i < TokenTable.MAX_OPERAND && operand[i] != null;i++)
		{
			if(operand[i].contentEquals("X"))
			{
				setFlag(TokenTable.xFlag,1);
			}
		}
		
		//b,p,e Flag Setting
		if(operator.startsWith("+"))
		{

			setFlag(TokenTable.bFlag | TokenTable.pFlag,0);
			setFlag(TokenTable.eFlag,1);
		}
		else
		{
			setFlag(TokenTable.bFlag | TokenTable.eFlag,0);
			setFlag(TokenTable.pFlag,1);
		}

	}
	
	/** 
	 * n,i,x,b,p,e flag�� �����Ѵ�. 
	 * 
	 * ��� �� : setFlag(nFlag, 1); 
	 *   �Ǵ�     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : ���ϴ� ��Ʈ ��ġ
	 * @param value : ����ְ��� �ϴ� ��. 1�Ǵ� 0���� �����Ѵ�.
	 */
	public void setFlag(int flag, int value) {
		//...
	
		if(value == 1)
		{
			nixbpe =  (char)(nixbpe |flag);
		}
		else
			nixbpe = (char) (nixbpe ^ (nixbpe & (char)flag));
	}
	
	/**
	 * ���ϴ� flag���� ���� ���� �� �ִ�. flag�� ������ ���� ���ÿ� �������� �÷��׸� ��� �� ���� �����ϴ� 
	 * 
	 * ��� �� : getFlag(nFlag)
	 *   �Ǵ�     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : ���� Ȯ���ϰ��� �ϴ� ��Ʈ ��ġ
	 * @return : ��Ʈ��ġ�� �� �ִ� ��. �÷��׺��� ���� 32, 16, 8, 4, 2, 1�� ���� ������ ����.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}
	

	
}
