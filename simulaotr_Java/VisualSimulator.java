package SP20_simulator;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다.<br>
 * 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트 하는 역할을 수행한다.<br>
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator extends JFrame {
	static String instFile = System.getProperty("user.dir")+"/inst.data";
	
	ResourceManager resourceManager = new ResourceManager();
	SicLoader sicLoader = new SicLoader(resourceManager);
	InstLuncher instLuncher = new InstLuncher(resourceManager,instFile);
	SicSimulator sicSimulator = new SicSimulator(resourceManager,instLuncher);
	
	//SimulatorScreen simsScreen = new SimulatorScreen();
	
	
	//프로그램 화면 구성 
	//OpenFile
	private JLabel labFileName;
	private JTextField tfFileName;
	private JButton btnFileOpen;
	
	//Header Record
	private JPanel panHeader;
	private JLabel labProgName;
	private JLabel labStAddress;
	private JLabel labProgLen;
	private JTextField tfProgName;
	private JTextField tfStAddress;
	private JTextField tfProgLen;
	
	
	//End Record
	private JPanel panEnd;
	private JLabel labFstExecAddress;
	private JTextField tfFstExeAddress;
	
	
	//Register SIC + SIC/XE
	private JPanel panAllReg;
	private JLabel labRegA;
	private JLabel labRegX;
	private JLabel labRegL;
	private JLabel labRegPC;
	private JLabel labRegSW;
	private JLabel labRegDec;
	private JLabel labRegHex;
	private JTextField tfDecA, tfHexA;
	private JTextField tfDecX, tfHexX;
	private JTextField tfDecL, tfHexL;
	private JTextField tfDecPC, tfHexPC;
	private JTextField tfHexSW;
	
	//Register Only SIC/XE
	private JPanel panXeReg;
	private JLabel labRegB;
	private JLabel labRegS;
	private JLabel labRegT;
	private JLabel labRegF;
	private JLabel labXeDec;
	private JLabel labXeHex;
	private JTextField tfDecB, tfHexB;
	private JTextField tfDecS, tfHexS;
	private JTextField tfDecT, tfHexT;
	private JTextField tfHexF;
	
	
	
	//Simulate Result 
	private JLabel labRelocAddr;
	private JLabel labTargAddr;
	private JLabel labInst;
	private JLabel labUsingDevice;
	private JTextField tfRelocAddr;
	private JTextField tfTargAddr;
	private JTextField tfUsingDevice;
	private DefaultListModel dlmoInst;
	private JScrollPane spInst;
	private JList<String> taInst; 
	private JButton btnExecStep;
	private JButton btnExecAll;
	private JButton btnExit;
	
	//Log
	private JLabel labLog;
	private JScrollPane spLog;
	private JPanel panLog;
	private DefaultListModel dlmoLog;
	private JList<String> taLog;
	
	
	//Frame
	private JFrame curFrame;
	
	//Open File Path
	public String strOpenPath;
	
	
	public VisualSimulator(String frameTitle) {
		initSimulatorScreen(frameTitle);
	}
	
	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	
	
	public void load(File program){
		//...
		resourceManager.initializeResource();
		sicLoader.initSicLoader(resourceManager);
		sicLoader.load(program);
		sicSimulator.initSicSimulaotr(resourceManager, instLuncher);
		sicSimulator.setLoadFailFlag(sicLoader.getLoadFailFlag());
		sicSimulator.load(program);
	};

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep(){

		sicSimulator.oneStep();
		if(sicSimulator.getExitFlag())
		{
			this.btnExecStep.setEnabled(false);
			this.btnExecAll.setEnabled(false);
		}
	};

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep(){
		sicSimulator.allStep();
		if(sicSimulator.getExitFlag())
		{
			this.btnExecStep.setEnabled(false);
			this.btnExecAll.setEnabled(false);
		}
	};
	
	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	
	public void update(){
		setRegisterInfo();
		addInstList();
		addLogList();
		setTargetAddr();
		setUsingDevice();
	};
	
	
	public void allUpdate() {
		setRegisterInfo();
		addResidualAllInstList();
		addResidualAllLogList();
		setTargetAddr();
		setUsingDevice();
	}

	public void terminate() {
		resourceManager.closeDevice();
		System.exit(0);
	}
	
	public static void main(String[] args) {
			VisualSimulator vis = new VisualSimulator(new String("Simulaotr for SIC/XE"));
	}
	
	public void initSimulatorScreen(String screenTitle) {
		this.setTitle(screenTitle);
		setSize(600,700);
		setLocationRelativeTo(null);
		setLayout(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		curFrame = this;
		initLabel();
		initButton();
		initTextComponent();
		initPanel();
		setVisible(true);
	}
	
	private void initLabel()
	{
		labFileName = new JLabel("FileName:");
		
		labProgName = new JLabel("<html>Program Name: </html>");
		labStAddress = new JLabel("<html>Start Address of<br> Object Program:</html>");
		labProgLen = new JLabel("Length of Program:") ;
		
		labFstExecAddress = new JLabel("<html>Address of First Instruction<br> in ObjectProgram:</html>");
		labRegA = new JLabel("A(#0)");
		labRegX = new JLabel("X(#1)");
		labRegL = new JLabel("L(#2)");
		labRegPC = new JLabel("PC(#8)");
		labRegSW = new JLabel("SW(#9)");
		labRegDec = new JLabel("Dec");
		labRegHex = new JLabel("Hex");
		labRegB = new JLabel("B(#3)");
		labRegS = new JLabel("S(#4)");
		labRegT = new JLabel("T(#5)");
		labRegF = new JLabel("F(#6)");
		labXeDec = new JLabel("Dec");
		labXeHex = new JLabel("Hex");
		labRelocAddr = new JLabel("Start Address in Memory");
		labTargAddr = new JLabel("Target Address:");
		labInst = new JLabel("Instructions");
		labUsingDevice = new JLabel("사용중인 장치");
		labLog = new JLabel("Log(명령어 수행 관련):");
		
		labFileName.setSize(100,50);
		labFileName.setLocation(10,0);
		add(labFileName);
		
		labProgName.setSize(100,50);
		labProgName.setLocation(10,0);
		labStAddress.setSize(100,50);
		labStAddress.setLocation(10,40);
		labProgLen.setSize(110,50);
		labProgLen.setLocation(10,80);
		
		labRegDec.setSize(40,50);
		labRegDec.setLocation(80,5);
		labRegHex.setSize(40,50);
		labRegHex.setLocation(170,5);
		labRegA.setSize(40,50);
		labRegA.setLocation(10,30);
		labRegX.setSize(40,50);
		labRegX.setLocation(10,50);
		labRegL.setSize(40,50);
		labRegL.setLocation(10,70);
		labRegPC.setSize(50,50);
		labRegPC.setLocation(8,90);
		labRegSW.setSize(50,50);
		labRegSW.setLocation(8,110);
		
		labXeDec.setSize(40,50);
		labXeDec.setLocation(80,2);
		labXeHex.setSize(40,50);
		labXeHex.setLocation(170,2);
		labRegB.setSize(40,50);
		labRegB.setLocation(10,23);
		labRegS.setSize(40,50);
		labRegS.setLocation(10,43);
		labRegT.setSize(40,50);
		labRegT.setLocation(10,63);
		labRegF.setSize(50,50);
		labRegF.setLocation(8,83);
		
		labFstExecAddress.setSize(200,30);
		labFstExecAddress.setLocation(10,20);
	
		labRelocAddr.setSize(150,50);
		labRelocAddr.setLocation(300,80);
	
		labTargAddr.setSize(100,50);
		labTargAddr.setLocation(300,127);
	
		labInst.setSize(100,50);
		labInst.setLocation(300,145);
		
		labUsingDevice.setSize(100,50);
		labUsingDevice.setLocation(450,163);
		
		
		labLog.setSize(300,50);
		labLog.setLocation(10,420);
	}
	
	private void initButton()
	{
		btnFileOpen = new JButton("open");
		btnExecStep = new JButton("실행(1 Step)");
		btnExecAll = new JButton("실행(All)");
		btnExit = new JButton("종료"); 
		
		btnExecStep.setEnabled(false);
		btnExecAll.setEnabled(false);
		
		ActionListener alsn = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton clicked = (JButton)e.getSource();
				if(clicked.getText().contentEquals("open"))
				{
					FileDialog fdigOpen = new FileDialog(curFrame,"열기",FileDialog.LOAD);
					fdigOpen.setVisible(true);
					strOpenPath = fdigOpen.getDirectory() + fdigOpen.getFile();
					tfFileName.setText(fdigOpen.getFile());
					File file = new File(strOpenPath); 
					load(file);
					initScreenAfterLoad();
					btnExecStep.setEnabled(true);
					btnExecAll.setEnabled(true);
				}
			}
		};
		btnFileOpen.addActionListener(alsn);
		btnFileOpen.setSize(70,30);
		btnFileOpen.setLocation(275,10);
		add(btnFileOpen);
		
		alsn = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton clicked = (JButton)e.getSource();
				
				if(clicked.getText().contentEquals("종료"))
					terminate();
			}
		};
		btnExit.addActionListener(alsn);
		
		alsn = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton clicked = (JButton)e.getSource();
				oneStep();
				update();
			}
		};
		
		btnExecStep.addActionListener(alsn);
		btnExecStep.setSize(110,30);
		btnExecStep.setLocation(450,320);
		
		
		alsn = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton clicked = (JButton)e.getSource();
				allStep();
				allUpdate();
			}
		};
		btnExecAll.addActionListener(alsn);
		btnExecAll.setSize(110,30);
		btnExecAll.setLocation(450,360);
		
		btnExit.setSize(110,30);
		btnExit.setLocation(450,400);
	}
	
	private void initTextComponent()
	{
		tfFileName = new JTextField();
		tfProgName = new JTextField();
		tfStAddress = new JTextField();
		tfProgLen = new JTextField();
		tfFstExeAddress = new JTextField();
		tfDecA = new JTextField();
		tfHexA = new JTextField();
		tfDecX = new JTextField(); 
		tfHexX = new JTextField();
		tfDecL = new JTextField();
		tfHexL = new JTextField();
		tfDecPC = new JTextField();
		tfHexPC = new JTextField();
		tfHexSW = new JTextField();
		tfDecB = new JTextField();
		tfHexB = new JTextField();
		tfDecS = new JTextField();
		tfHexS = new JTextField();
		tfDecT = new JTextField();
		tfHexT = new JTextField();
		tfHexF = new JTextField();
		tfRelocAddr = new JTextField();
		tfTargAddr = new JTextField();
		tfUsingDevice = new JTextField();
		dlmoInst = new DefaultListModel();
		taInst  = new JList<String>(dlmoInst);
		dlmoLog = new DefaultListModel();
		taLog = new JList<String>(dlmoLog);
		
		tfRelocAddr.setText("0");
		
		tfFileName.setSize(200,30);
		tfFileName.setLocation(70,10);
		add(tfFileName);
		
		tfProgName.setSize(130,25);
		tfProgName.setLocation(110,20);
		tfStAddress.setSize(115,25);
		tfStAddress.setLocation(110,55);
		tfProgLen.setSize(115,25);
		tfProgLen.setLocation(120,90);
		
		tfDecA.setSize(75,20);
		tfDecA.setLocation(74,44);
		tfHexA.setSize(75,20);
		tfHexA.setLocation(167,44);
		
		tfDecX.setSize(75,20);
		tfDecX.setLocation(74,64);
		tfHexX.setSize(75,20);
		tfHexX.setLocation(167,64);

		tfDecL.setSize(75,20);
		tfDecL.setLocation(74,84);
		tfHexL.setSize(75,20);
		tfHexL.setLocation(167,84);
		
		tfDecPC.setSize(75,20);
		tfDecPC.setLocation(74,104);
		tfHexPC.setSize(75,20);
		tfHexPC.setLocation(167,104);
		
		tfHexSW.setSize(168,20);
		tfHexSW.setLocation(74,124);
		
		tfDecB.setSize(75,20);
		tfDecB.setLocation(74,37);
		tfHexB.setSize(75,20);
		tfHexB.setLocation(167,37);

		tfDecS.setSize(75,20);
		tfDecS.setLocation(74,57);
		tfHexS.setSize(75,20);
		tfHexS.setLocation(167,57);
		
		tfDecT.setSize(75,20);
		tfDecT.setLocation(74,78);
		tfHexT.setSize(75,20);
		tfHexT.setLocation(167,78);
		
		tfHexF.setSize(168,20);
		tfHexF.setLocation(74,99);
		
		tfFstExeAddress.setSize(120,20);
		tfFstExeAddress.setLocation(120,35);
		
		tfRelocAddr.setSize(170,20);
		tfRelocAddr.setLocation(400,118);
		
		tfTargAddr.setSize(150,20);
		tfTargAddr.setLocation(400,142);
		
		
		taInst.setSize(130,250);
		//taInst.setLocation(300,180);
		//LineBorder br = new LineBorder(Color.black);
		//taInst.setBorder(br);
		
		//taLog.setSize(130,250);
		
		tfUsingDevice.setSize(100,20);
		tfUsingDevice.setLocation(447,200);
	}
	

	private void initPanel()
	{
		TitledBorder tb = new TitledBorder(new LineBorder(Color.gray),"H(Header Record)");
		tb.setTitleColor(Color.black);
		panHeader = new JPanel();
		panHeader.setLayout(null);
		panHeader.setSize(270,120);
		panHeader.setBorder(tb);
		panHeader.setLocation(10,40);
		panHeader.add(labProgName);
		panHeader.add(tfProgName);
		panHeader.add(labStAddress);
		panHeader.add(tfStAddress);
		panHeader.add(labProgLen);
		panHeader.add(tfProgLen);
		add(panHeader);
		
		tb = new TitledBorder(new LineBorder(Color.gray),"Register");
		tb.setTitleColor(Color.black);
		panAllReg = new JPanel();
		panAllReg.setLayout(null);
		panAllReg.setBorder(tb);
		panAllReg.setSize(270,150);
		panAllReg.setLocation(10,160);
		panAllReg.add(labRegDec);
		panAllReg.add(labRegHex);
		panAllReg.add(labRegA);
		panAllReg.add(labRegX);
		panAllReg.add(labRegL);
		panAllReg.add(labRegPC);
		panAllReg.add(labRegSW);
		panAllReg.add(tfDecA);
		panAllReg.add(tfHexA);
		panAllReg.add(tfDecX);
		panAllReg.add(tfHexX);
		panAllReg.add(tfDecL);
		panAllReg.add(tfHexL);
		panAllReg.add(tfDecPC);
		panAllReg.add(tfHexPC);
		panAllReg.add(tfHexSW);
		add(panAllReg);
		

		tb = new TitledBorder(new LineBorder(Color.gray),"Register(for XE)");
		tb.setTitleColor(Color.black);
		panXeReg = new JPanel();
		panXeReg.setLayout(null);
		panXeReg.setBorder(tb);
		panXeReg.setSize(270,122);
		panXeReg.setLocation(10,310);

		panXeReg.add(labXeDec);
		panXeReg.add(labXeHex);
		panXeReg.add(labRegB);
		panXeReg.add(labRegS);
		panXeReg.add(labRegT);
		panXeReg.add(labRegF);

		panXeReg.add(tfDecB);
		panXeReg.add(tfHexB);
		panXeReg.add(tfDecS);
		panXeReg.add(tfHexS);
		panXeReg.add(tfDecT);
		panXeReg.add(tfHexT);
		panXeReg.add(tfHexF);
		add(panXeReg);

		tb = new TitledBorder(new LineBorder(Color.gray),"E(End Record)");
		tb.setTitleColor(Color.black);
		panEnd = new JPanel();
		panEnd.setLayout(null);
		panEnd.setSize(270,60);
		panEnd.setBorder(tb);
		panEnd.setLocation(300,40);
		panEnd.add(labFstExecAddress);
		panEnd.add(tfFstExeAddress);
		
		add(panEnd);
		add(labRelocAddr);
		add(tfRelocAddr);

		add(labTargAddr);
		add(tfTargAddr);
		
		
	//	tb = new TitledBorder(new LineBorder(Color.gray),"Log(명령 수행 관련)");
	//	tb.setTitleColor(Color.black);
		spLog = new JScrollPane(taLog);
		//spLog.setLayout(null);
	//	spLog.setBorder(tb);
		spLog.setSize(560,190);
		spLog.setLocation(10,455);
		
		add(spLog);
		add(labLog);
		/*panLog = new JPanel();
		panLog.setLayout(null);
		panLog.setBorder(tb);
		panLog.setSize(560,210);
		panLog.setLocation(10,435);
		
		panLog.add(this.taLog)
		
		add(panLog);
		*/
		spInst = new JScrollPane(taInst);
		spInst.setSize(130,250);
		spInst.setLocation(300,180);
		add(labInst);
		add(spInst);
		
		//add(taInst);
		
		add(labUsingDevice);
		add(tfUsingDevice);
		
		add(btnExecStep);
		add(btnExecAll);
		add(btnExit);
	}
	
	public void initScreenAfterLoad() {
		setHeaderInfo();
		setRegisterInfo();
		setFstExecStAddr();
		setRelocAddr();
		initInstList();
		initLogList();
		setTargetAddr();
		setUsingDevice();

	}
	
	public void setHeaderInfo() {
		String progName = sicSimulator.getProgName();
		String stAddr = String.format("%06X", sicSimulator.getStAddr());
		String progLength = String.format("%06X",sicSimulator.getProgLength());
		
		tfProgName.setText(progName);
		tfStAddress.setText(stAddr);
		tfProgLen.setText(progLength);
	}
	
	public void setRegisterInfo() {
		int val = sicSimulator.getRegister(ResourceManager.A);
		
		tfDecA.setText(String.format("%d", val));
		tfHexA.setText(String.format("%06X", val));
		
		val = sicSimulator.getRegister(ResourceManager.X);
		
		tfDecX.setText(String.format("%d", val));
		tfHexX.setText(String.format("%06X", val));
		
		val = sicSimulator.getRegister(ResourceManager.L);
		
		tfDecL.setText(String.format("%d", val));
		tfHexL.setText(String.format("%06X", val));
		
		val = sicSimulator.getRegister(ResourceManager.PC);
		
		tfDecPC.setText(String.format("%d", val));
		tfHexPC.setText(String.format("%06X", val));
		
		val = sicSimulator.getRegister(ResourceManager.SW);
		
		tfHexSW.setText(String.format("%06X", val));

		
		val = sicSimulator.getRegister(ResourceManager.B);
		
		tfDecB.setText(String.format("%d", val));
		tfHexB.setText(String.format("%06X", val));

		val = sicSimulator.getRegister(ResourceManager.S);
		
		tfDecS.setText(String.format("%d", val));
		tfHexS.setText(String.format("%06X", val));

		val = sicSimulator.getRegister(ResourceManager.T);
		
		tfDecT.setText(String.format("%d", val));
		tfHexT.setText(String.format("%06X", val));

		val = sicSimulator.getRegister(ResourceManager.F);
		
		tfHexF.setText(String.format("%06X", val));
	}
	
	public void setFstExecStAddr() {
		int fstExecAddr = sicSimulator.getExecStAddr();
		
		tfFstExeAddress.setText(String.format("%06X", fstExecAddr));
	}
	
	public void setTargetAddr() {
		int targAddr = sicSimulator.getTargAddr();
		if(targAddr < 0)
			tfTargAddr.setText("");
		else
			tfTargAddr.setText(String.format("%06X",targAddr));
	}
	
	public void setUsingDevice() {
		int device = sicSimulator.getUsingDevice();
		if(device < 0)
			tfUsingDevice.setText("");
		else
			tfUsingDevice.setText(String.format("%02X", device));
		
	}
	
	public void setRelocAddr() {
		String relocAddr = Integer.toString(sicSimulator.getRelocAddr());
		
		tfRelocAddr.setText(relocAddr);
	}
	
	public void initInstList() {
		if(this.dlmoInst.getSize() > 0)
			this.dlmoInst.clear();
		addInstList();
	}
	
	public void addInstList() {
		if(sicSimulator.getExitFlag())
			return;
		
		this.dlmoInst.addElement(sicSimulator.getCurObjectCode());
	}
	
	public void addResidualAllInstList() {
		List<String> instList = sicSimulator.getResidualObCode();
		
		for(int i=0;i<instList.size();i++)
		{
			this.dlmoInst.addElement(instList.get(i));
		}
	}
	
	public void initLogList() {
		if(this.dlmoLog.getSize() > 0)
			this.dlmoLog.clear();
	}
	
	
	public void addLogList() {
		this.dlmoLog.addElement(sicSimulator.getCurLog());
	}
	public void addResidualAllLogList() {
		List<String> logList = sicSimulator.getResidualLog();
		
		for(int i=0;i<logList.size();i++)
		{
			this.dlmoLog.addElement(logList.get(i));
		}
	}
	
}
