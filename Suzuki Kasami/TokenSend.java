import java.io.ObjectOutputStream;
import java.net.Socket;

public class TokenSend implements Runnable{
	final Socket s;
	int a=0,pid;	
	Token t;
	NBInfo[] nblist = new NBInfo[10];
	ObjectOutputStream oos;
	public TokenSend(Socket s,ObjectOutputStream oos, Token t) 
	{
		this.s = s;
		this.t = t;
		this.oos = oos;
	}
	
	public void  run() 
	{
		
		try {
				oos.writeObject(t);
				oos.close();
				oos.flush();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}
}
