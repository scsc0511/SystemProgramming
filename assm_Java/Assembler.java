import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Assembler : 
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. 
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. 
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. 
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) 
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) 
 * 
 * 
 * �ۼ����� ���ǻ��� : 
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.
 *  2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)
 * 
 *     
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
	/** instruction ���� ������ ���� */
	InstTable instTable;
	/** �о���� input ������ ������ �� �� �� �����ϴ� ����. */
	ArrayList<String> lineList;
	/** ���α׷��� section���� symbol table�� �����ϴ� ����*/
	ArrayList<SymbolTable> symtabList;
	/** ���α׷��� section���� literal table�� �����ϴ� ����*/
	ArrayList<LiteralTable> literaltabList;
	/** ���α׷��� section���� ���α׷��� �����ϴ� ����*/
	ArrayList<TokenTable> TokenList;
	/** 
	 * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����.   
	 * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
	 */
	ArrayList<String> codeList;
	
	int sectCtr;
	ArrayList<ArrayList<String>> ereftabList;
	ArrayList<ExtdefTable>	edeftabList;
	ArrayList<Integer>	sectSize;
	Boolean existLtrg;
	
	public enum Directives{START,END,BYTE,WORD,RESB,RESW,CSECT,EXTDEF,EXTREF,EQU,ORG,LTORG};
	
	/**
	 * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
	 * 
	 * @param instFile : instruction ���� �ۼ��� ���� �̸�. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		literaltabList = new ArrayList<LiteralTable>();
		TokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
		sectCtr = 0;
		sectSize = new ArrayList<Integer>();
		existLtrg = false;

		ereftabList = new ArrayList<ArrayList<String>>();
		edeftabList = new ArrayList<ExtdefTable>();
	}


	
	
	/** 
	 * ������� ���� ��ƾ
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler(System.getProperty("user.dir")+"/inst.data");
		assembler.loadInputFile(System.getProperty("user.dir")+"/input.txt");	
		assembler.pass1();

		assembler.printSymbolTable("symtab_20160337");
		assembler.printLiteralTable("literaltab_20160337");
		assembler.pass2();
		assembler.printObjectCode("output_20160337");

		
	}

	/**
	 * inputFile�� �о�鿩�� lineList�� �����Ѵ�.
	 * @param inputFile : input ���� �̸�.
	 */
	private void loadInputFile(String inputFile) {
		// TODO Auto-generated method stub
		try {
			File srcFile = new File(inputFile);
			FileReader srcFileReader = new FileReader(srcFile);
			BufferedReader srcBufReader = new BufferedReader(srcFileReader);
			String srcLine = "";
			while((srcLine = srcBufReader.readLine()) != null)
			{
				String listLine = new String(srcLine);
				lineList.add(listLine);
			}
			srcBufReader.close();
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}	
	}

	private void tokenize() {
		
		TokenTable curTokTab = new TokenTable(new SymbolTable(),instTable);
		Token curToken = null;
		int curId;
		String curLine = null;
		
		TokenList.add(curTokTab);
		
		for(int i=0 ;i<lineList.size();i++)
		{
			curLine = lineList.get(i);
			if(curLine.startsWith("."))		continue;
			curTokTab.putToken(curLine);
			curId = curTokTab.getLastId();
			curToken = curTokTab.getToken(curId);
			if(curToken.operator.contentEquals("CSECT"))
			{
				
				curTokTab.removeToken(curId);
				if(curTokTab.litFlag && !curTokTab.ltrgFlag)
					curTokTab.putToken("	LTORG");
				curTokTab = new TokenTable(new SymbolTable(),instTable);
				curTokTab.putToken(curLine);
				TokenList.add(curTokTab);
			}
			if(curToken.operator.contentEquals("END") 
			&& !curTokTab.ltrgFlag && curTokTab.litFlag)
			{
				curTokTab.removeToken(curId);
				curTokTab.putToken("	LTORG");
				curTokTab.putToken(curLine);		
			}
			if(curToken.operator.contentEquals("LTORG"))
				curTokTab.ltrgFlag = true;
			for(int j=0;j<curToken.operand.length && curToken.operand[j]!=null;j++)
			{
				if(curToken.operand[j].startsWith("="))
				{
					curTokTab.litFlag = true;
					break;
				}
			}
		}	
		
	}
	
	private void setSymbolTable() {
		TokenTable curTokTab = null;
		Token curToken = null;
		SymbolTable curSymTab = null;
	
		//Symbol Table�� Symbol �߰� 
		for(int i=0;i<TokenList.size() ; i++)
		{
			curTokTab = TokenList.get(i);
			curSymTab = curTokTab.symTab;
			if(curSymTab == null)
			{
				curTokTab.symTab = new SymbolTable();
				curSymTab = curTokTab.symTab;
			}
			
			curToken = null;
			for(int j=0;j<curTokTab.len;j++)
			{
				curToken = curTokTab.getToken(j);
				if(curToken.label.length() > 0)
					curSymTab.putSymbol(curToken.label, -1);
			}
			symtabList.add(curSymTab);
		}
	
	}
	
	
	private int constantSize(String constant)
	{
		char type = constant.charAt(0);
		String val;
		if(type != 'C' && type != 'X')
		{
			type = 0;
			val = constant;
		}
		else
			val = constant.substring(2,constant.length()-1); 	
		
		
		if(type == 'C')
			return val.length();
		else if(type == 'X')
			return val.length()/2 + val.length()%2;
		
		return 3; 
	}
	
	
	private void setLiteralTable() {
		TokenTable curTokTab = null;
		Token curToken = null;
		LiteralTable curLitTab = null;
		String[] opr = null;
		
		for(int i=0;i<TokenList.size();i++)
		{
			curTokTab = TokenList.get(i);
			curLitTab = curTokTab.literalTab;
			if(curLitTab == null)
			{
				curTokTab.literalTab = new LiteralTable();
				curLitTab = curTokTab.literalTab;
			}
			for(int j=0;j<curTokTab.len;j++)
			{
				curToken = curTokTab.getToken(j);
				opr = curToken.operand;
				if(opr[0]!=null && opr[0].startsWith("="))
					curLitTab.putLiteral(opr[0],-1);
			}
			literaltabList.add(curLitTab);
				
		}
	}
	
	private void modifyCurSectLiteralTable(Token curTok,int sectCtr)
	{
		try {
			LiteralTable litTab = literaltabList.get(sectCtr);
			curTok.byteSize = 0;
			String curLit;
			for(int i=0;i<litTab.len;i++)
			{
				curLit = litTab.getLiteral(i);
				litTab.modifyLiteral(curLit, curTok.location + curTok.byteSize);
				curTok.byteSize += constantSize(curLit.substring(1));
			}
		}catch(NullPointerException e) {
			return ;
		}
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
	
	public  int byteArrayToInt(byte bytes[]) 
	{
		return ((((int)bytes[0] & 0xff) << 24) |
			(((int)bytes[1] & 0xff) << 16) |
			(((int)bytes[2] & 0xff) << 8) |
			(((int)bytes[3] & 0xff)));
	} 


	private byte[] getValueOfOperand(String operand,int sectCtr)
	{
		if(operand.startsWith("C"))
		{
			String value = operand.substring(2,operand.length()-2);
			return value.getBytes();
		}
		else if(operand.startsWith("X"))
		{
			String value = operand.substring(2,operand.length()-2);
			int parseVal = Integer.parseInt(value,16);
			return intToByteArray(parseVal);
		}
		else if(isNumeric(operand)||isNumeric(operand.substring(1)))
		{
			String value = operand; 
			if(operand.startsWith("#"))
				value = operand.substring(1);
			int parseVal = Integer.parseInt(value);
			return intToByteArray(parseVal);
		}
		else if(operand.startsWith("="))
		{
			LiteralTable curliTab = literaltabList.get(sectCtr);
			String litVal = operand.substring(3,operand.length()-2);
			int location = curliTab.search(litVal);
			return intToByteArray(location);
		}
		else
		{
			SymbolTable cursymTab = symtabList.get(sectCtr);
			int location = cursymTab.search(operand);
			return intToByteArray(location);
		}
			
			
	}
	
	private int calculateExpression(String[] operand,int sectCtr)
	{
		String[] strVals;
		byte[] byteVal;
		int intVal = 0;
		boolean pFlag = true;
		
		if(operand[0].contains("+"))
			strVals = operand[0].split("+");
		else if(operand[0].contains("-"))
		{
			strVals = operand[0].split("-");
			pFlag = false;
		}
		else
			return -1;
		byteVal = getValueOfOperand(strVals[0],sectCtr);
		intVal = byteArrayToInt(byteVal);
		byteVal = getValueOfOperand(strVals[1],sectCtr);
		if(pFlag)
			intVal += byteArrayToInt(byteVal);
		else
			intVal -= byteArrayToInt(byteVal);
		
		return intVal;
	}
	
	
	private void setAddressWithDirective(Token curTok,int sectCtr)
	{
		Directives curDirc = Directives.valueOf(curTok.operator);
		TokenTable curtTab = TokenList.get(sectCtr);
		ExtdefTable curdTab = null;
		SymbolTable symTab = null;
		ArrayList<String> cureTab = null;
		int opValue;
		
		switch(curDirc) {
		case START:
			curTok.location = Integer.parseInt(curTok.operand[0]);
			curTok.byteSize = 0;
			sectCtr = 0;
			
			ereftabList.add(new ArrayList<String>());
			edeftabList.add(new ExtdefTable());
			curdTab = edeftabList.get(sectCtr);
			curdTab.putSymbol(curTok.label, curTok.location);
			
		//	symtabList.add(new SymbolTable());
			symTab = symtabList.get(sectCtr);
			symTab.modifySymbol(curTok.label, curTok.location);
		
			existLtrg = false;
			break;
		case END:
		//	if(!existLtrg)
		//		modifyCurSectLiteralTable(curTok,sectCtr);
		//	else
				curTok.byteSize = 0;
			break;
		case BYTE:
			curTok.byteSize = constantSize(curTok.operand[0]);
			break;
		case WORD:
			curTok.byteSize = 3;
			break;
		case RESB:
			curTok.byteSize = Integer.parseInt(curTok.operand[0]);
			break;
		case RESW:
			curTok.byteSize = Integer.parseInt(curTok.operand[0])*3;
			break;
		case CSECT:
			curTok.location = 0;
			curTok.byteSize = 0;
			
			ereftabList.add(new ArrayList<String>());
			edeftabList.add(new ExtdefTable());
			curdTab = edeftabList.get(sectCtr);
			curdTab.putSymbol(curTok.label, curTok.location);
			
			symTab = symtabList.get(sectCtr);
			symTab.modifySymbol(curTok.label, curTok.location);
			break;
		case EXTDEF:
			curTok.byteSize = 0;
			curdTab = edeftabList.get(sectCtr);
			for(int i=0;i<curTok.operand.length && curTok.operand[i]!=null;i++)
				curdTab.putSymbol(curTok.operand[i], -1);
			curtTab.setExtDefTable(curdTab);
			break;
		case EXTREF:
			curTok.byteSize = 0;
			cureTab = ereftabList.get(sectCtr);
			for(int i=0;i<curTok.operand.length && curTok.operand[i] != null ;i++)
				cureTab.add(curTok.operand[i]);
			curtTab.setExtRefTable(cureTab);
			break;
		case EQU:
			curTok.byteSize = 0;
			symTab = symtabList.get(sectCtr);
			if(curTok.operand[0].contentEquals("*"))	
			{
				symTab.modifySymbol(curTok.label,curTok.location);
				break;
			}
			opValue = calculateExpression(curTok.operand,sectCtr);
			if(opValue < 0)
				opValue = byteArrayToInt(getValueOfOperand(curTok.operand[0],sectCtr));
			curTok.location = opValue;
			symTab.modifySymbol(curTok.label, opValue);
			break;
		case ORG:
			curTok.byteSize = 0;
			opValue = calculateExpression(curTok.operand,sectCtr);
			if(opValue<0)
				opValue = byteArrayToInt(getValueOfOperand(curTok.operand[0],sectCtr));
			curTok.location = opValue;
			break;
		case LTORG:
			if(literaltabList.size() <= sectCtr+1)
				literaltabList.add(new LiteralTable());
			modifyCurSectLiteralTable(curTok,sectCtr);
			existLtrg = true;
			break;
		}
	}
	
	private void setAddress()
	{
		
		TokenTable curTokTab = null;
		Token curTok = null;
		Token prevTok = null;
		ExtdefTable curDefTab = null;
		int opCode;
		int size;
		
		for(int i=0;i<TokenList.size();i++)
		{
			curTokTab = TokenList.get(i);
			size = 0;
			for(int j=0;j<curTokTab.len;j++)
			{
				
				curTok = curTokTab.getToken(j);
				opCode = curTokTab.getOpcode(curTok.operator);
				if(j!=0)
				{
					prevTok = curTokTab.getToken(j-1);
					curTok.location = prevTok.location + prevTok.byteSize;
					if(curTok.label.length() > 0)
					{
						SymbolTable symTab = symtabList.get(i);
						symTab.modifySymbol(curTok.label,curTok.location);
						curDefTab = edeftabList.get(i);
						curDefTab.modifyLoc(curTok.label, curTok.location);
					}
				
				}
				if(opCode< 0)		//Directive
				{
					setAddressWithDirective(curTok,i);
				}
				else				//Instruction
				{
					Instruction inst = instTable.search(curTok.operator);
					curTok.byteSize = inst.format;
					
					if(inst.format == 3 && curTok.operator.startsWith("+"))
						curTok.byteSize = 4;
				}
			
				size += curTok.byteSize;
			}
			sectSize.add(size);
			if(!existLtrg)
				modifyCurSectLiteralTable(curTok,i);		
			existLtrg = false;
		}
	}
	
	/** 
	 * pass1 ������ �����Ѵ�.
	 *   1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����
	 *   2) label�� symbolTable�� ����
	 *   
	 *    ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
	 */
	private void pass1() {
		// TODO Auto-generated method stub
		
		tokenize();
		
		setSymbolTable();
		setLiteralTable();
		
		setAddress();
	
	}
	
	/**
	 * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printSymbolTable(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
		SymbolTable cursymTab = null;
		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(int i=0;i<symtabList.size();i++)
			{
				String output = null;
				cursymTab = symtabList.get(i);
				for(int j=0;j<cursymTab.len;j++)
				{
					String symbol = cursymTab.getSymbol(j);
					int location = cursymTab.search(symbol);
					output = String.format("%s	%X\n", symbol,location);
					
					bw.write(output);
				}
				output = "\n";
				bw.write(output);;
				bw.flush();
			}
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �ۼ��� LiteralTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printLiteralTable(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
		LiteralTable curlitTab = null;
		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(int i=0;i<literaltabList.size();i++)
			{
				String output = null;
				curlitTab = literaltabList.get(i);
				if(curlitTab.len < 1)	continue;
				for(int j=0;j<curlitTab.len;j++)
				{
					String literal = curlitTab.getLiteral(j);
					int location = curlitTab.search(literal);
					output = String.format("%s	%X\n", literal,location);
					
					bw.write(output);
				}
				bw.flush();	
			}
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * pass2 ������ �����Ѵ�.
	 *   1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
	 */
	private void pass2() {
		// TODO Auto-generated method stub
		TokenTable curTokTab = null;
		Token curTok = null;
		
		for(int i=0;i<TokenList.size();i++)
		{
			curTokTab = TokenList.get(i);
			for(int j=0;j<curTokTab.len;j++)
			{
				curTokTab.makeObjectCode(j);
			}
			curTok = curTokTab.getToken(0);
			curTok.objectCode = curTok.objectCode + String.format("%06X", sectSize.get(i));
		}
	}
	
	/**
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub
		File file = new File(fileName);
		TokenTable curTokTab = null;
		Token curTok = null;
		int textRecordSize = 0;
		int locctr = 0;
		int stLoc;
		boolean skipFlag;
		String textRecord;
		
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	
			for(int i=0;i<TokenList.size();i++)
			{
				curTokTab = TokenList.get(i);
				textRecord = new String("");
				textRecordSize = 0;
				locctr = 0;
				stLoc = 0;
				skipFlag = false;
				for(int j=0;j<curTokTab.len;j++)
				{
					curTok = curTokTab.getToken(j);
				
					if(curTok.operator.contentEquals("END"))
						break;
					
					if(curTok.objectCode.contentEquals(""))
						skipFlag = true;
					else
						skipFlag = false;
					
					if(curTok.operator.contentEquals("START")
					|| curTok.operator.contentEquals("CSECT")
					|| curTok.operator.contentEquals("EXTDEF")
					|| curTok.operator.contentEquals("EXTREF"))
						bw.write(curTok.objectCode+"\n");
					else if(textRecordSize+curTok.byteSize <= 30 
						&& ((locctr) == curTok.location) && (!skipFlag))
					{
						textRecord = textRecord + curTok.objectCode;
						textRecordSize = textRecordSize + curTok.byteSize;
					}
					else if(!skipFlag)
					{
						textRecord = "T"+ String.format("%06X", stLoc)
								    + String.format("%02X", textRecordSize)
									+ textRecord+"\n";
						bw.write(textRecord);
						
						stLoc = curTok.location;
						textRecordSize =curTok.byteSize;
						textRecord = curTok.objectCode;
					}	
					if(!skipFlag)
						locctr += curTok.byteSize;
				}
				bw.write("T"+String.format("%06X", stLoc)+String.format("%02X", textRecordSize)
						+textRecord+"\n");
				for(int j=0;j<curTokTab.modifRecordList.size();j++)
				{
					bw.write(curTokTab.modifRecordList.get(j)+"\n");
				}
				if(i==0)
				{
					curTok = curTokTab.getToken(0);
					bw.write("E"+String.format("%06X", Integer.parseInt(curTok.operand[0]))+"\n\n");;
				}
				else
					bw.write("E\n\n");
			}
			
			bw.flush();
			bw.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
}
