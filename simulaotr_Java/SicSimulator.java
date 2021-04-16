package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * �ùķ����ͷμ��� �۾��� ����Ѵ�. VisualSimulator���� ������� ��û�� ������ �̿� ����
 * ResourceManager�� �����Ͽ� �۾��� �����Ѵ�.  
 * 
 * �ۼ����� ���ǻ��� : <br>
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� ������ ��.<br>
 *  2) �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.<br>
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.<br>
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)<br>
 * 
 * <br><br>
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class SicSimulator {	
	ResourceManager rMgr;
	InstLuncher instLuncher;
	ProgramInformation info;
	boolean exitFlag;
	boolean loadFailure;
	
	static final int END_LOC = 65536;
	static final int EXIT_FLAG = 0;
	
	
	public SicSimulator(ResourceManager resourceManager, InstLuncher instLuncher) {
		// �ʿ��ϴٸ� �ʱ�ȭ ���� �߰�
		this.rMgr = resourceManager;
		this.instLuncher = instLuncher;
		this.info = new ProgramInformation();
		this.exitFlag = false;
		this.loadFailure = false;
	}

	public void initSicSimulaotr(ResourceManager resourceManager, InstLuncher instLuncher)
	{
		this.rMgr = resourceManager;
		this.instLuncher = instLuncher;
		this.info = new ProgramInformation();
		this.exitFlag = false;
		this.loadFailure = false;
	}
	/**
	 * ��������, �޸� �ʱ�ȭ �� ���α׷� load�� ���õ� �۾� ����.
	 * ��, object code�� �޸� ���� �� �ؼ��� SicLoader���� �����ϵ��� �Ѵ�. 
	 */
	
	public void load(File program) {
		/* �޸� �ʱ�ȭ, �������� �ʱ�ȭ ��*/
		//�������� �ʱ�ȭ
		rMgr.setRegister(ResourceManager.A, 0);
		rMgr.setRegister(ResourceManager.X, 0);
		rMgr.setRegister(ResourceManager.B, 0);
		rMgr.setRegister(ResourceManager.L, 0);				
		rMgr.setRegister(ResourceManager.SW, ResourceManager.NONE_FLAG);
		rMgr.setRegister(ResourceManager.S, 0);
		rMgr.setRegister(ResourceManager.T, 0);
		rMgr.setRegister(ResourceManager.F, 0);
		rMgr.setRegister(ResourceManager.PC, 0);
		
		try {
			if(loadFailure)
				throw new Exception();
				
			FileReader rd = new FileReader(program);
			BufferedReader bfReader = new BufferedReader(rd);
			String rdData;
			while((rdData=bfReader.readLine()) != null)
			{
				//Header�� ��� 
				if(rdData.startsWith("H")) {
					if(getStAddr() < 0)
					{
						setProgName(rdData.substring(1,7));
						setStAddr(Integer.parseInt(rdData.substring(7,13)));
					}
					updateProgLength(Integer.parseInt(rdData.substring(13,19),16));
				}
				
				//END�� ��� 
				if(rdData.startsWith("E") && getStAddr() < 0)
				{
					setExecStAddr(Integer.parseInt(rdData.substring(1,6)));
				}
			}
			setRelocAddr(rMgr.lockMem.get(getProgName())[0]);	
			//rMgr.setRegister(ResourceManager.L, getRelocAddr() + getExecStAddr());
			rMgr.setRegister(ResourceManager.PC, getRelocAddr());

			updateProgramInfo();
			
			rd.close();	
		}catch(IOException e) {
			e.printStackTrace();
		}catch(Exception e) {
			info.addLog("Log Failure");
		}

	}
	
	public void setLoadFailFlag(boolean loadFail) {
		loadFailure = loadFail;
	}
	
	/**
	 * 1���� instruction�� ����� ����� ���δ�. 
	 */
	public void oneStep() {
		
		try {
			if(exitFlag||loadFailure)	return;
			instLuncher.execInstruction();
			setExitFlag();
			if(exitFlag)	return;
			updateProgramInfo();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ���� ��� instruction�� ����� ����� ���δ�.
	 */
	public void allStep() {
		while(!exitFlag&&!loadFailure) {
			oneStep();
		}
	}
	
	public void setExitFlag() {
		int addr = rMgr.getRegister(ResourceManager.PC);
		if(addr==EXIT_FLAG)
			exitFlag = true;
	}
	
	public boolean getExitFlag() {
		return this.exitFlag;
	}
	
	/**
	 * �� �ܰ踦 ������ �� ���� ���õ� ����� ���⵵�� �Ѵ�.
	 */
	//��ɾ� �ϳ��� ������ �Ŀ� ȣ���ؾ� �� 
	public void updateProgramInfo() {
		//���� ���  ProgramInformation �ν��Ͻ��� ����
		
		//Object Code �߰�
		addObjectCode();		
		//Register ���� 
		updateAllRegisterInfo();
		//ObCodeIndex ����
		//increaseObCodeIndex();
		//Log �߰� 
		addLog();
		
		//Target Address ���� 
		setTargAddr(instLuncher.getTargAddr());
		
		//Using Device ���� 
		setUsingDevice(instLuncher.getUsingDevice());
	}
	
	public void addObjectCode() {
		//ó�� op code �ε� 
		int opCode = rMgr.charToInt(
							rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 1)
						);
		
		//object code�� ���� �ޱ� 
		int format = instLuncher.getFormat(opCode&(0xFC));
		
		//object code �ޱ� 
		char[] objectCode = rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), format);
		int intObjectCode = rMgr.charToInt(objectCode);
		
		
		//4�����̸� 1 Byte �߰��ؼ� �޾ƿ���
		if((format==3) && ((intObjectCode&0x1000)>0))
		{
			objectCode = rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 4);
			format = 4;
		}
		//object code ��ȯ 
		intObjectCode = rMgr.charToInt(objectCode);
		
		
		//ó�� Object Code Program Info�� ���� 
		info.addObjectCode(intObjectCode, format);
	}
	
	public void addLog() {
		int opCode = rMgr.charToInt(
							rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 1)
						);
		String log = instLuncher.getOpName(opCode&(0xFC));
		
		info.addLog(log); 
	}
	
	public void updateAllRegisterInfo() {
		//Register ���� �߰� 
		info.setRegister(ResourceManager.A, rMgr.getRegister(ResourceManager.A));
		info.setRegister(ResourceManager.X, rMgr.getRegister(ResourceManager.X));
		info.setRegister(ResourceManager.B, rMgr.getRegister(ResourceManager.B));
		info.setRegister(ResourceManager.L, rMgr.getRegister(ResourceManager.L));
		info.setRegister(ResourceManager.S, rMgr.getRegister(ResourceManager.S));
		info.setRegister(ResourceManager.T, rMgr.getRegister(ResourceManager.T));
		info.setRegister(ResourceManager.F, rMgr.getRegister(ResourceManager.F));
		info.setRegister(ResourceManager.PC, rMgr.getRegister(ResourceManager.PC));
		info.setRegister(ResourceManager.SW, rMgr.getRegister(ResourceManager.SW));
	}
	
	public void increaseObCodeIndex() {
		info.increaseObCodeIndex();
	}
	
	public String getProgName() {
		return info.getProgName();
	}
	
	public void setProgName(String progName) {
		info.setProgName(new String(progName));
	}
	public void updateProgLength(int addVal) {
		int curLength = info.getProgLength(); 
		
		info.setProgLength(curLength + addVal);;
	}
	
	public int getStAddr() {
		return info.getStAddr();
	}
	
	public void setStAddr(int stAddr) {
		info.setStAddr(stAddr);
	}
	
	public void setRelocAddr(int addr) {
		info.setRelocAddr(addr);
	}
	
	public void setExecStAddr(int execStAddr) {
		info.setExecStAddr(execStAddr);
	}
	
	public void setTargAddr(int targAddr) {
		info.setTargetAddr(targAddr);
	}
	
	public void setUsingDevice(int usingDev) {
		info.setUsingDevice(usingDev);
	}
	
	public int getRelocAddr() {
		return info.getRelocAddr();
	}
	
	public int getProgLength() {
		return info.getProgLength();
	}
	
	public int getRegister(int regNum) {
		return info.getRegister(regNum);
	}
	
	public int getExecStAddr() {
		return info.getExecStAddr();
	}
	
	public ArrayList<String> getAllObjectCode(){
		return info.getAllObjectCode();
	}
	
	public String getLastObjectCode() {
		return info.getLastObjectCode();
	}
	
	public ArrayList<String> getAllLog(){
		return info.getAllLog();
	}
	
/*	public String getLastLog() {
		return info.getLastLog();
	}*/
	
 	public String getCurObjectCode() {
 		return info.getCurObjectCode();
 	}
	
	public String getCurLog() {
		return info.getCurLog();
	}
	
	public int getTargAddr() {
		return info.getTargAddr();
	}
	
	public int getUsingDevice() {
		return info.getUsingDevice();
	}
	
	public int getObCodeIndex() {
		return info.getObCodeIndex();
	}
	
	public int getLogIndex() {
		return info.getLogIndex();
	}
	
 	public List<String> getResidualObCode(){
 		List<String> residual = info.getResidualObCode();
 		
 		return residual;
 	}
 	
 	public List<String> getResidualLog(){
 		List<String> residual = info.getResidualLog();
 		
 		return residual;
 	}
}

class ProgramInformation{
	private String progName;
	private int relocAddr;
	private int stAddr;
	private int execStAddr;
	private int progLength;
	private ArrayList<String> objectCodeList;
	private ArrayList<String> logList;
	private int[] register;
	private int targAddr;
	private int usingDev;
	
	int obCodeIndex;
	int logIndex;
	
 	public ProgramInformation() {
 		progName = null;
 		relocAddr = -1;
 		stAddr = -1;
 		progLength = 0;
 		objectCodeList = new ArrayList<String>();
 		logList = new ArrayList<String>();
 		register = new int[10];
 		obCodeIndex = -1;
 		logIndex = 0;
 		targAddr = -1;
 		usingDev = -1;
 	}
 	
 	public void setProgName(String progName) {
 		this.progName = progName;
 	}
 	
 	public void setRelocAddr(int relocAddr) {
 		this.relocAddr = relocAddr;
 	}
 	
 	public void setStAddr(int stAddr) {
 		this.stAddr = stAddr;
 	}
 	
 	public void setExecStAddr(int execStAddr) {
 		this.execStAddr = execStAddr;
 	}
 	
 	public void setProgLength(int progLength) {
 		this.progLength = progLength;
 	}
 	
 	public void setTargetAddr(int targAddr) {
 		this.targAddr = targAddr;
 	}
 	
 	public void setUsingDevice(int usingDev) {
 		this.usingDev = usingDev;
 	}
 	
 	public void addObjectCode(byte[] objectCode) {
 		String addObjectCode = new String(objectCode);
 		
 		this.objectCodeList.add(addObjectCode);
 		
 	}
 	
 	public void addObjectCode(char[] objectCode) {
 		String addObjectCode = new String(objectCode);
 		this.objectCodeList.add(addObjectCode);
 	}
 	
 	public void addObjectCode(int objectCode, int format) {
 		String addObjectCode = String.format("%06X", objectCode);
 		
 		if(format == 1)
 			addObjectCode = String.format("%02X", objectCode&0xFF);
 		else if(format == 2)
 			addObjectCode = String.format("%04X", objectCode&0xFFFF);
 		else if(format == 3)
 			addObjectCode = String.format("%06X", objectCode&0xFFFFFF);
 		else if(format ==4)
 			addObjectCode = String.format("%08X", objectCode&0xFFFFFFFF);
 		
 		this.objectCodeList.add(addObjectCode);
 	}
 	
 	public void addLog(String log) {
 		this.logList.add(log);
 	}
 	
 	public void setRegister(int regNum, int val) {
 		register[regNum] = val;
 	}
 	
 	public String getProgName() {
 		return this.progName;
 	}
 	
 	public int getRelocAddr() {
 		return this.relocAddr;
 	}
 	
 	public int getStAddr() {
 		return this.stAddr;
 	}
 	
 	public int getExecStAddr() {
 		return this.execStAddr;
 	}
 	
 	public int getProgLength() {
 		return this.progLength;
 	}
 	
 	public void increaseObCodeIndex(){
 		obCodeIndex++;
 	}
 	
 	public void increaseLogIndex(){
 		logIndex++;
 	}
 	
 	public ArrayList<String> getAllObjectCode(){
 		return this.objectCodeList;
 	}
 	
 	public String getCurObjectCode() {
 		this.increaseObCodeIndex();
 		return this.getOneObjectCode(this.getObCodeIndex());
 	}
 	
 	public String getLastObjectCode() {
 		return this.objectCodeList.get(objectCodeList.size()-1);
 	}
 	
 	public String getOneObjectCode(int index) {
 		return this.objectCodeList.get(index);
 	}
 	
 	public ArrayList<String> getAllLog(){
 		return this.logList;
 	}
 	
 	public List<String> getResidualObCode(){
 		this.increaseObCodeIndex();
 		List<String> residual = this.objectCodeList.subList(this.getObCodeIndex()
 							  , this.objectCodeList.size());
 		
 		return residual;
 	}
 	
 	public List<String> getResidualLog(){
 		List<String> residual = this.logList.subList(this.getLogIndex(), logList.size());
 		this.increaseLogIndex();
 		
 		
 		return residual;
 	}
 	
 	
 	
 /*	public String getLastLog() {
 		return this.logList.get(logList.size()-1);
 	}*/
 	
 	public String getLog(int index) {
 		return this.logList.get(index);
 	}

 	public String getCurLog() {
 		String curLog = this.getLog(this.getLogIndex());
 		this.increaseLogIndex();
 		return curLog;
 	}
 	
 	public String getLastLog() {
 		return this.logList.get(logList.size()-2);
 	}
 	
 	public int getRegister(int regNum) {
 		return register[regNum];
 	}
 	
 	public int getObCodeIndex() {
 		return obCodeIndex;
 	}

 	public int getLogIndex() {
 		return logIndex;
 	}

 	
 	public int getTargAddr() {
 		return this.targAddr;
 	}
 	
 	public int getUsingDevice() {
 		return this.usingDev;
 	}
 	
 	
}
