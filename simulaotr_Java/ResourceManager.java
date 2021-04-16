package SP20_simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



/**
 * ResourceManager�� ��ǻ���� ���� ���ҽ����� �����ϰ� �����ϴ� Ŭ�����̴�.
 * ũ�� �װ����� ���� �ڿ� ������ �����ϰ�, �̸� ������ �� �ִ� �Լ����� �����Ѵ�.<br><br>
 * 
 * 1) ������� ���� �ܺ� ��ġ �Ǵ� device<br>
 * 2) ���α׷� �ε� �� ������ ���� �޸� ����. ���⼭�� 64KB�� �ִ밪���� ��´�.<br>
 * 3) ������ �����ϴµ� ����ϴ� �������� ����.<br>
 * 4) SYMTAB �� simulator�� ���� �������� ���Ǵ� �����͵��� ���� ������. 
 * <br><br>
 * 2���� simulator������ ����Ǵ� ���α׷��� ���� �޸𸮰����� �ݸ�,
 * 4���� simulator�� ������ ���� �޸� �����̶�� ������ ���̰� �ִ�.
 */
public class ResourceManager{
	/**
	 * ����̽��� ���� ����� ��ġ���� �ǹ� ������ ���⼭�� ���Ϸ� ����̽��� ��ü�Ѵ�.<br>
	 * ��, 'F1'�̶�� ����̽��� 'F1'�̶�� �̸��� ������ �ǹ��Ѵ�. <br>
	 * deviceManager�� ����̽��� �̸��� �Է¹޾��� �� �ش� �̸��� ���� ����� ���� Ŭ������ �����ϴ� ������ �Ѵ�.
	 * ���� ���, 'A1'�̶�� ����̽����� ������ read���� ������ ���, hashMap�� <"A1", scanner(A1)> ���� �������μ� �̸� ������ �� �ִ�.
	 * <br><br>
	 * ������ ���·� ����ϴ� �� ���� ����Ѵ�.<br>
	 * ���� ��� key������ String��� Integer�� ����� �� �ִ�.
	 * ���� ������� ���� ����ϴ� stream ���� �������� ����, �����Ѵ�.
	 * <br><br>
	 * �̰͵� �����ϸ� �˾Ƽ� �����ؼ� ����ص� �������ϴ�.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	ArrayList<String> lockDev /*= new ArrayList<String>()*/; 
	HashMap<String,Integer[]> lockMem /*= new HashMap<String,Integer[]>()*/;
	char[] memory = new char[65536]; // String���� �����ؼ� ����Ͽ��� ������.
	int[] register = new int[10];
	
	SymbolTable symtabList;
	
	// �̿ܿ��� �ʿ��� ���� �����ؼ� ����� ��.
	static final int A = 0;
	static final int X = 1;
	static final int L = 2;
	static final int B = 3;
	static final int S = 4;
	static final int T = 5;
	static final int F = 6;
	static final int PC = 8;
	static final int SW = 9;
	static final int NONE_FLAG = 0;
	static final int SIGN_FLAG = 1;
	static final int ZERO_FLAG = 2;
	static final int BIG_FLAG = 4;

	static final int REG_SIZE = 3;
	
	static final String ROOT_PATH = System.getProperty("user.dir");
	static final String INPUT_DEV = "F1";
	static final String OUTPUT_DEV = "05";
	
	
	/**
	 * �޸�, �������͵� ���� ���ҽ����� �ʱ�ȭ�Ѵ�.
	 */
	public ResourceManager() {
		initializeResource();
	}
	
	public void initializeResource(){
		try {
			closeDevice();
			lockDev = new ArrayList<String>(); 
			lockMem = new HashMap<String,Integer[]>();
			deviceManager.put(INPUT_DEV, new FileInputStream(ROOT_PATH+"/"+INPUT_DEV));
			deviceManager.put(OUTPUT_DEV, new FileOutputStream(ROOT_PATH+"/"+OUTPUT_DEV));
			symtabList = new SymbolTable();
			this.allocMemory("EXIT", 1, 0);
		}catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * deviceManager�� �����ϰ� �ִ� ���� ����� stream���� ���� �����Ű�� ����.
	 * ���α׷��� �����ϰų� ������ ���� �� ȣ���Ѵ�.
	 */
	public void closeDevice() {
		Set sKeySet = deviceManager.keySet(); 
		Iterator<String> i = sKeySet.iterator();
		String curKey;
		
		while(i.hasNext())
		{
			curKey = i.next();
			while(lockDev.contains(curKey));
			deviceManager.remove(curKey);
			i = sKeySet.iterator();
		}
	}
	
	public void putSymbol(String name, int addr) {
		symtabList.putSymbol(name, addr);
	}
	
	public int symbolSearch(String symbol) {
		return symtabList.search(symbol);
	}
	
	public boolean hasUndefinedSymbol() {
		return symtabList.hasUndefinedSymbol();
	}
	
	/**
	 * ����̽��� ����� �� �ִ� ��Ȳ���� üũ. TD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * ����� stream�� ���� deviceManager�� ���� ������Ų��.
	 * @param devName Ȯ���ϰ��� �ϴ� ����̽��� ��ȣ,�Ǵ� �̸�
	 */
	public void testDevice(String devName) {
		//����̽��� �����ϴ��� Ȯ�� 
		if(!deviceManager.containsKey(devName)) {
			register[SW] = ZERO_FLAG;
			return ;
		}
		//����̽� lock ���� Ȯ�� 
		if(/*devName.contentEquals(INPUT_DEV) &&*/ lockDev.contains(devName)) {
			register[SW] = ZERO_FLAG;
			return;
		}
		else if(/*devName.contentEquals(OUTPUT_DEV) &&*/ lockDev.contains(devName)) {
			register[SW] = ZERO_FLAG;
			return;
		}
		//����̽� ��� ���� 
		register[SW] = SIGN_FLAG;
	}
	

	/**
	 * ����̽��κ��� ���ϴ� ������ŭ�� ���ڸ� �о���δ�. RD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param num �������� ������ ����
	 * @return ������ ������
	 */
	public char[] readDevice(String devName, int num){
		byte[] readByteData = new byte[num];
		char[] readCharData = new char[num];
		String toConv;
		
		try {
			if(!devName.contains(INPUT_DEV))
				throw new Exception();
			lockDev.add(INPUT_DEV);
			FileInputStream rd = (FileInputStream)deviceManager.get(devName);
			rd.read(readByteData, 0, num);
			toConv = new String(readByteData, "UTF-8");
			readCharData = toConv.toCharArray();
			lockDev.remove(INPUT_DEV);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return readCharData;
		
	}

	/**
	 * ����̽��� ���ϴ� ���� ��ŭ�� ���ڸ� ����Ѵ�. WD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param data ������ ������
	 * @param num ������ ������ ����
	 */
	public void writeDevice(String devName, char[] data, int num){
		String toConv;
		byte[] baWrData = new byte[data.length];
		int tmp;
		byte btmp;
		try {
			if(!devName.contains(OUTPUT_DEV))
				throw new Exception();
			lockDev.add(OUTPUT_DEV);
			FileOutputStream wr = (FileOutputStream)deviceManager.get(devName);
			//baWrData = SicLoader.convertCharArrToByteArr(data);
			//int tmp;
			for(int i=0;i<data.length;i++)
			{
				tmp = (byte)(baWrData[i]|((data[i]&0xF00)>>4));
				btmp = (byte)(baWrData[i]|((data[i]&0xF00)>>4));
				baWrData[i] = (byte)(data[i]&(0xF));
				baWrData[i] = (byte) (baWrData[i]|((data[i]&0xF00)>>4));
			}
			baWrData = Arrays.copyOfRange(baWrData, baWrData.length-num, baWrData.length);
			wr.write(baWrData, 0, num);
			wr.flush();
			lockDev.remove(OUTPUT_DEV);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getAvailMemoryAddr(int progSize) {
		Iterator<String> it = ResourceManager.sortByValue(lockMem).iterator();
		String curKey;
		Integer[] prevProg = new Integer[] {0,0};
		Integer[] curProg = new Integer[2];
		
		while(it.hasNext() && (prevProg[1]+progSize)<65536) {
			curKey = it.next();
			curProg = lockMem.get(curKey);
			if(curProg[0]-prevProg[1] >= progSize)
			{
				break;
			}
			prevProg = curProg;
		}
		
		if(prevProg[1]+progSize < 65536)
		{
			return prevProg[1];
		}
		return -1;
	
		
	}
	
	public void allocMemory(String progName,int progSize, int allocAddr) {
	
		Integer[] procSect = new Integer[] {allocAddr, allocAddr+progSize};
		lockMem.put(progName, procSect);
	}
	
	
	/**
	 *Ư�� �޸� ������ �Ҵ��ϱ� ���� � �޸� ������ �Ҵ����� �˾Ƴ��� ���� ������ �Ҵ� �� �޸� ������ ���� �ּҸ� �������� 
	 *���� �ϱ����� HashMap�� ������ �ʿ䰡 �ִµ� �̷��� ����� �������ִ� �Լ� 
	 **/
	public static List sortByValue(HashMap<String,Integer[]> map) {
		List<String> listKey = new ArrayList();
		listKey.addAll(map.keySet());
		
		Collections.sort(listKey,new Comparator() {
			public int compare(Object o1, Object o2) {
				Object[] v1 = map.get(o1);
				Object[] v2 = map.get(o2);
				
				return ((Comparable)v2[0]).compareTo(v1[0]);
			}
		});
		
		Collections.reverse(listKey);
		return listKey;
	}
	
	public void freeMemory(String progName)
	{
		lockMem.remove(progName);
	}
	
	/**
	 * �޸��� Ư�� ��ġ���� ���ϴ� ������ŭ�� ���ڸ� �����´�.
	 * @param location �޸� ���� ��ġ �ε���
	 * @param num ������ ����
	 * @return �������� ������
	 */
	public char[] getMemory(int locate, int num){
		char[] caReadData = Arrays.copyOfRange(memory, locate, locate+num);

		
	

		return caReadData;
	}

	/**
	 * �޸��� Ư�� ��ġ�� ���ϴ� ������ŭ�� �����͸� �����Ѵ�. 
	 * @param locate ���� ��ġ �ε���
	 * @param data �����Ϸ��� ������
	 * @param num �����ϴ� �������� ����
	 */
	public void setMemory(int locate, char[] data, int num){

		for(int i=0,j=data.length-num;j<data.length;i++,j++)
		{
			memory[locate+i] = (char)data[j];
		}
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ͱ� ���� ��� �ִ� ���� �����Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum �������� �з���ȣ
	 * @return �������Ͱ� ������ ��
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ϳ� ���ο� ���� �Է��Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum ���������� �з���ȣ
	 * @param value �������Ϳ� ����ִ� ��
	 */
	public void setRegister(int regNum, int value){
		register[regNum] = value;
	}

	/*
	 * ��ȣ�� �ش��ϴ� ���������� ������ ũ�� ��ŭ�� �����͸� 
	 * ������ �ּ��� �޸𸮿�  ����
	 * @param regNum ���������� �з� ��ȣ
	 * @param location ���� ���� �޸��� ���� ��ġ
	 * @param len �������Ϳ��� �о�� �������� ũ��
	 */
	public void storeRegister(int regNum, int locate, int num)
	{
		char[] caData = intToChar(getRegister(regNum));
		
		setMemory(locate, caData, num);
	}
	/*
	*/
	public void loadMemory(int regNum, int locate, int num)
	{
		try {
			if(num>REG_SIZE)
				throw new IllegalArgumentException();
			int loadVal = charToInt(getMemory(locate,num));
			setRegister(regNum, loadVal);
		}catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. int���� char[]���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		char[] caData = new char[4];

		caData[0] = (char) ((data & 0xF0000000)>>20);
		caData[0] |= (char) ((data & 0xF000000)>>24);
		caData[1] = (char) ((data & 0xF00000)>>12);
		caData[1] |= (char) ((data & 0xF0000)>>16);
		caData[2] = (char) ((data & 0xF000)>>4);
		caData[2] |= (char) ((data & 0xF00)>>8);
		caData[3] = (char) ((data & 0xF0)<<4);
		caData[3] |= (char) (data & 0xF);
		
		return caData;
	}

	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. char[]���� int���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public int charToInt(char[] data){
		int iData = 0;
		int tmp = 0;
		try {
			if(data.length > 4)
				throw new IllegalArgumentException();
			for(int i=0;i<data.length;i++)
			{
				iData = iData << 8;
				iData = (iData) |  (data[i] & 0xFF);
				tmp = (data[i] & 0xFF);
				iData = (iData) |  (((data[i]>>8) & 0xFF)<<4);
				tmp = ((((data[i]>>8) & 0xFF)<<4));
				
			}
		}catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		
		return iData;
	}

}