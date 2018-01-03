import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.Serializable;

public  class TokenRequest implements Runnable,Serializable{
	Socket sss;
	ObjectOutputStream dss;
	Token m;
	int a=0,pid,seq_num=0,lclock=0,fmc=0;  
	public TokenRequest(Socket sss, ObjectOutputStream dss, Token m)
	{
		this.dss = dss;
		this.sss = sss;
		this.m = m;
	}
	
	public void  run() 
	{
		
		try {
				dss.writeObject(m);
				dss.flush();
								
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}


}
