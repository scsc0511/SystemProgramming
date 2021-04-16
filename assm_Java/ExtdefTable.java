import java.util.ArrayList;


public class ExtdefTable {

	ArrayList<String>	symList;
	ArrayList<Integer>	locList;
	int len;
	
	
	public ExtdefTable()
	{
		symList = new ArrayList<String>();
		locList = new ArrayList<Integer>();
		len = 0;
	}
	
	
	public void putSymbol(String sym, int location)
	{
		symList.add(sym);
		locList.add(location);
		len++;
	}
	
	public void modifyLoc(String sym, int location)
	{
		int id = symList.indexOf(sym);
		if(id>-1)
			locList.set(id, location);
	}
	
	
	public String getSymbol(int id)
	{
		return symList.get(id);
	}
	
	public int getAddr(String sym)
	{
		int id = symList.indexOf(sym);
		
		return locList.get(id);
	}
	
	
}
