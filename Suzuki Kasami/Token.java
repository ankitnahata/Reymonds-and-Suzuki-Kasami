import java.io.Serializable;
import java.time.Instant;

class Token implements Serializable{
	String message=null;
	int[] queue = new int[30];
	int[] LN = new int[30];
	Instant leavetime;
	
	Long avgsync=0L;
	Long avgwait=0L;
	int avgmessages=0;
	int crisecnum=0;
	public void enqueue(int[] temparr)
	{
		int i=0;
		while(queue[i]!=0)
		{
			i++;
		}
		for(int j=1;j<temparr.length;j++)
		{
			if(temparr[j]!=0)
			{
				queue[i]=j;
				i++;
			}
			
		}
	}
	public int dequeue()
	{
		int rettemp = queue[0];
		for(int ii=0;ii<queue.length-1;ii++)
		{
			queue[ii]=queue[ii+1];
		}
		return rettemp;
	}
}
