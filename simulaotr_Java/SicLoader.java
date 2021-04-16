package SP20_simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다. 
 * <br><br>
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다.<br>
 * - program code를 메모리에 적재시키기<br>
 * - 주어진 공간만큼 메모리에 빈 공간 할당하기<br>
 * - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	static final int HEADER_LEN = 20;
	static final int EXTDEF_SYM_LEN = 12;
	static final int EXTREF_SYM_LEN = 6;
	
	ResourceManager rMgr;
	ArrayList<String> progName;
	int relocAddr;
	int stAddr;
	int progSize;
	
	boolean loadFailure = false;
	
	
	public SicLoader(ResourceManager resourceManager) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
		progName = new ArrayList<String>();
		stAddr = -1;
	}

	public void initSicLoader(ResourceManager resourceManager) {

		setResourceManager(resourceManager);
		progName = new ArrayList<String>();
		stAddr = -1;
	}
	
	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr=resourceManager;
	}
	
	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다.
	 * load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode){
		//Ascii 형식을 Internal 형식으로 바꿈
		//16진수를 10진수로 바꾸지는 않음 
		try {
			
			//pass1
			pass1(objectCode);
			if(rMgr.hasUndefinedSymbol())
				throw new IllegalArgumentException();
			//pass2
			pass2(objectCode);
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	};
	
	private ArrayList<Byte> readOneRecord(FileInputStream rd)
	{
		ArrayList<Byte> record = new ArrayList<Byte>();
		byte[] readByte = new byte[1];
		
		try {
			while(rd.read(readByte,0,1)>-1)
			{
				if(readByte[0] == (byte)('\n'&0xFF))
					break;
				record.add(readByte[0]);
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return record;
	}
	
	private void pass1(File objectCode) {
		
		FileInputStream rd;
		ArrayList<Byte> readData;
		byte[] realData;
		boolean flag = false;
		byte pad[] = new byte[1];
		char tmp = '0';
		
		try {
			//pass1
			rd = new FileInputStream(objectCode);
					
			while(true)
			{
				//Read One Record From Object Code File
				readData = readOneRecord(rd);
				
				//Cannot Read From Object Code File -> Terminate Pass1 
				if(readData.isEmpty()) 
					break;
				
				//Parsing Read Record

				//Convert Byte Array To byte Array
				realData = convertByteArrtobyteArr(readData.toArray(new Byte[readData.size()]));
				tmp = (char)(realData[0]);
				//Terminate Parsing for One Section 
				if(realData[0] == (byte)('E'&0xFF)) {
					/*
						if(realData.length>1)
						{
							realData = Arrays.copyOfRange(realData, 1, realData.length);
							stAddr = unpackingHexByteToInt(realData) + relocAddr;
						}else if(stAddr<0)
						{
							stAddr = rMgr.lockMem.get(progName.get(0))[0];
						}*/
						rd.read(pad,0,1);
						
				}
					
				//Parsing Header Record 
				if(realData[0] == (byte)('H'&0XFF))
					parseHeaderRecord(realData);
				
				//Define External Symbol
				if(realData[0] == (byte)('D'&0XFF))
					parseDefineRecord(realData);
				if(realData[0] == (byte)('R'&0xFF))
					parseReferRecord(realData);
				//Ignore External Reference Record, Text Record and Modificaton Record in pass 1 
			}
			rd.close();
		}catch(IOException e) {
			e.printStackTrace();
		}

	}
	
	private void pass2(File objectCode) {
		
		FileInputStream rd;
		ArrayList<Byte> readData;
		byte[] realData;
		byte[] toConv;
		char[] toLoad;
		char[] memVal;
		
		int sectLen;
		int textStAddr;
		int textSize;
		int modifStAddr;
		int modifLen;
		char modifFlag;
		String modifSym;
		String curProgName;
		
		try {
			//pass2
			rd = new FileInputStream(objectCode);
					
			while(true)
			{
				//Read One Record From Object Code File
				readData = readOneRecord(rd);
				
				//Cannot Read From Object Code File -> Terminate Pass1 
				if(readData.isEmpty()) 
					break;
				
				//Parsing Read Record

				//Convert Byte Array To byte Array
				realData = convertByteArrtobyteArr(readData.toArray(new Byte[readData.size()]));

				//Remove Pad
				if(realData[0] == (byte)('E'&0xFF))
				{	
					if(realData.length>1)
					{
						realData = Arrays.copyOfRange(realData, 1, realData.length);
						rMgr.setRegister(ResourceManager.PC, unpackingHexByteToInt(realData) + relocAddr);
					}
					else if(stAddr<0)
					{
						rMgr.setRegister(ResourceManager.PC,rMgr.lockMem.get(progName.get(0))[0]);
					}
					rd.read(realData,0,1);
					continue;
				}
				
				//Parsing Header Record 
				if(realData[0] == (byte)('H'&0XFF))
				{
					//Set Start Address
					toConv = Arrays.copyOfRange(realData, 1, 7);
					curProgName = new String(unpackingCharByteToCharArr(toConv));
					stAddr = rMgr.lockMem.get(curProgName)[0];
					
					//Get Section Length 
					toConv = Arrays.copyOfRange(realData, 13, 19);
					sectLen = unpackingHexByteToInt(toConv);
				}
				
				//Process and Load Text Record
				if(realData[0] == (byte)('T'&0XFF))
				{
					//Get Start Address of Text Record
					toConv = Arrays.copyOfRange(realData, 1, 7);
					textStAddr = unpackingHexByteToInt(toConv) + stAddr;
					
					//Get Bytes(Hex) Length of Text Record
					toConv = Arrays.copyOfRange(realData, 7, 9);
					textSize = unpackingHexByteToInt(toConv);
					
					//Load Object Code to Memory 
					toConv = Arrays.copyOfRange(realData, 9, 9+2*textSize);
					toLoad = packingbyteArrToCharArr(toConv);

					rMgr.setMemory(textStAddr, toLoad, textSize); 
				}
				//Process Modification Record 
				if(realData[0] == (byte)('M'&0xFF))
				{
					//Get Start Address To Modify
					toConv = Arrays.copyOfRange(realData, 1, 7);
					modifStAddr = unpackingHexByteToInt(toConv) + stAddr;
					
					//Get Bytes(Hex) Length To Modify 
					toConv = Arrays.copyOfRange(realData,7,9);
					modifLen = unpackingHexByteToInt(toConv);
					if(modifLen %2 == 1)
						modifLen = modifLen/2 +1;
					else
						modifLen = modifLen/2;
					
					//Get Flag To Modify
					toConv = Arrays.copyOfRange(realData, 9, 10);
					modifFlag = (char)toConv[0];
					
					//Get Symbol Name To Modify
					toConv = Arrays.copyOfRange(realData,10,16);
					modifSym = new String(toConv).trim();
					
					//Get Values To Modify 
					memVal = rMgr.getMemory(modifStAddr, modifLen);
					//Modify Memory Value
					memVal = calculateModifFlag(memVal,modifSym,modifFlag);
					
					//Store Modified Value To Memory
					rMgr.setMemory(modifStAddr, memVal, modifLen);
					memVal = rMgr.getMemory(modifStAddr, modifLen);
				}
			}
			rd.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void parseHeaderRecord(byte[] realData)
	{
		byte[] toConv;
		
		try {
			//Parse Section Size 
			toConv = Arrays.copyOfRange(realData, 13, HEADER_LEN-1);
			progSize = unpackingHexByteToInt(toConv);
			
			//Parse Section Name 
			toConv = Arrays.copyOfRange(realData, 1, 7);
			progName.add(new String(unpackingCharByteToCharArr(toConv)));
			
			//Allocate Memory Address to Section
			relocAddr = rMgr.getAvailMemoryAddr(progSize);
			if(relocAddr < 0)
				throw new Exception();
			rMgr.allocMemory(progName.get(progName.size()-1), progSize, relocAddr);
			
			//Add Section Name as External Referencible Symbol to Resource Manager
			toConv = Arrays.copyOfRange(realData, 8, 13);
			relocAddr = relocAddr + unpackingHexByteToInt(toConv);
			rMgr.putSymbol(progName.get(progName.size()-1).trim(), relocAddr);
			//rMgr.symtabList.putSymbol(progName.get(progName.size()-1), relocAddr);
		}catch(Exception e) {
			e.printStackTrace();
			loadFailure = true;
		}
		
	}

	public boolean getLoadFailFlag() {
		return loadFailure;
	}
	
	private void parseDefineRecord(byte[] realData)
	{
		char[] extSymbol;
		int extSymbolAddr;
		
		for(int i=0;i<(realData.length-1)/12;i++)
		{
			extSymbol = unpackingCharByteToCharArr(
						Arrays.copyOfRange(realData,EXTDEF_SYM_LEN*i+1,EXTDEF_SYM_LEN*i+7)
						);
			extSymbolAddr = unpackingHexByteToInt(
					Arrays.copyOfRange(realData,EXTDEF_SYM_LEN*i+7,EXTDEF_SYM_LEN*(i+1)+1)
					) + relocAddr;
		
			rMgr.putSymbol(new String(extSymbol), extSymbolAddr);
			//rMgr.symtabList.putSymbol(new String(extSymbol), extSymbolAddr);
		}
	}
	
	private void parseReferRecord(byte[] realData)
	{
		char[] extSymbol;
		int extSymbolAddr;
		
		for(int i=0;i<(realData.length-1)/6;i++)
		{
			extSymbol = unpackingCharByteToCharArr(
						Arrays.copyOfRange(realData,EXTREF_SYM_LEN*i+1,EXTREF_SYM_LEN*i+7)
						);
			extSymbolAddr = -1;
		
			rMgr.putSymbol(new String(extSymbol), extSymbolAddr);
			//rMgr.symtabList.putSymbol(new String(extSymbol), extSymbolAddr);
		}
	}
	
	private char[] calculateModifFlag(char[] mem, String symName, char flag)
	{
		int memVal = rMgr.charToInt(mem);
		int symVal = rMgr.symbolSearch(symName.trim());
		//int symVal = rMgr.symtabList.search(symName);
		char[] retVal = null;
		
		try
		{
			if(symVal<0)
				throw new IllegalArgumentException();
			if(flag == '+')
				memVal = memVal + symVal;
			else if(flag == '-')
				memVal = memVal - symVal;
			else
				throw new IllegalArgumentException();
			retVal = rMgr.intToChar(memVal);
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	static byte[] attachByteArray(byte[] ba1, byte[] ba2)
	{
		byte[] attached = null;
		
		try {
			attached = new byte[ba1.length + ba2.length];
			System.arraycopy(ba1, 0, attached, 0, ba1.length);
			System.arraycopy(ba2, 0, attached, ba1.length, ba2.length);
		}catch(NullPointerException e) {
			e.printStackTrace();
		}catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		
		return attached;
	}
	
	static byte[] convertByteArrtobyteArr(Byte[] arrByte) {
		byte[] arrbyte = new byte[arrByte.length];
		
		for(int i=0;i<arrByte.length;i++)
		{
			arrbyte[i] = arrByte[i];
		}
		
		return arrbyte;
	}

	static char[] packingbyteArrToCharArr(byte[] arrbyte) {
		char[] charArr = new char[arrbyte.length/2 + arrbyte.length%2];
		char curVal;
		int tmp;
		try {
			
			for(int i=0;i<arrbyte.length/2;i++)
			{
				curVal = convertbyteAsciiToCharInternal(arrbyte[2*i]);
				tmp = curVal;
				charArr[i] = (char)(curVal<<8);
				curVal = convertbyteAsciiToCharInternal(arrbyte[2*i+1]);
				tmp = curVal;
				charArr[i] = (char)(charArr[i] + curVal);
			}
			if(arrbyte.length%2 == 1)
			{
				curVal = convertbyteAsciiToCharInternal(arrbyte[arrbyte.length-1]);
				charArr[charArr.length-1] = (char)(curVal<<8);
			}
			
		}catch(NullPointerException e) {
			e.printStackTrace();
			charArr = null;
		}
		
		return charArr;
	}
	
	static char convertbyteAsciiToCharInternal(byte ascii)
	{
		char internal = 0;
		try {
			if(ascii>=(byte)('0'&(0xFF)) && ascii<=(byte)('9'&(0xFF)))
				internal = (char)(ascii - ('0'&(0xFF)));
			else if(ascii>=(byte)('A'&(0xFF)) && ascii<=(byte)('F'&(0xFF)))
				internal = (char)(ascii - ('A'&(0xFF)) + 10);
			else 
				throw new IllegalArgumentException();

		}catch(IllegalArgumentException e) {
			e.printStackTrace();
			internal = 0;
		}
		
		return internal;
	}
	
	static byte[] convertCharArrToByteArr(char[] arrChar) {
		byte[] arrByte = new byte[arrChar.length*2];
		byte curVal;
		
		for(int i=0;i<arrChar.length;i++) {
			curVal = (byte)((arrChar[i] & (0xFF00))>>8);
			arrByte[2*i] = curVal;
			curVal = (byte)(arrChar[i] & (0xFF));
			arrByte[2*i+1] = curVal;
		}
		
		return arrByte;
	}
	
	static char[] unpackingCharByteToCharArr(byte[] baPacked) {
		char[] unpackResult = new char[baPacked.length];
		
		try {
			if(baPacked.length > 8)
				throw new IllegalArgumentException();
			for(int i=0;i<baPacked.length;i++)
			{
				unpackResult[i] = (char)(baPacked[i] & 0xFF);
			}
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		return unpackResult;
	}
	
	
	
	static int unpackingHexByteToInt(byte[] baPacked) {
		int unpackResult = 0;
		int curVal;
		try {
			for(int i=0;i<baPacked.length;i++)
			{
				unpackResult = unpackResult << 4;
				if(baPacked[i] >= 48 && (char)baPacked[i] <= 57)
					curVal = baPacked[i] - 48;
				else if(baPacked[i] >= 65 && baPacked[i] <= 70)
					curVal = baPacked[i] - 65 + 10;
				else if(baPacked[i] == 32 || baPacked[i] == 10)
					break;
				else
					throw new IllegalArgumentException();
				unpackResult = (unpackResult | (curVal & 0xF));  
			}
		}catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		
		return unpackResult;
	}
	

}
