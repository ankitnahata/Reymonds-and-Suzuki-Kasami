import java.net.*;
import java.util.Random;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.net.Socket;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.time.Duration;
import java.time.Instant;

import java.net.Socket;
public class process{
	public static void main(String[] args) throws Exception
	{	
		int procnum=0,temp=0;
		if(args.length >= 1 && args[0].equals("-c"))
			{
				String pid=null;
				int interval=0;
				int terminate=0;
				int t1=0,t2=0,t3=0;
				int[][] nb = new int[100][100];
				
				//Reading Input from dsConfig File

				String str = System.getProperty("user.dir");
		 		str = str + "//dsConfig1";
				File file = new File(str);
		 		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		 		String line = null;
		 		int i=0;
		 		while( (line = br.readLine())!= null )
		 		{
		               		String [] tokens = line.split("\\s+");
		 			if(i==0)
		 			{
		 				pid=tokens[1];
		 			}	
		 			else if(i==1)
		 			{
		 				procnum=Integer.parseInt(tokens[3]);
		 			}
		 			else if(i==2)
		 			{
		 				t1=Integer.parseInt(tokens[1]);	
		 				t2=Integer.parseInt(tokens[2]);	
		 			}
		 			else if(i==3)
		 			{
		 				t3=Integer.parseInt(tokens[1]);
		 			}	
		 			else if(i==4)
		 			{}
		 			else if((i>=5)&&(i<(5+procnum)))	
		 			{
		 				int tempj=0;
		 				while(tempj<tokens.length)
		 				{
		 					nb[i-5][tempj] = Integer.parseInt(tokens[tempj]);
		 					tempj++;
		 				}
		 			}
		 			i++;
		 			
		 		}

				//Output the Neighbour Table

		 		for(int tempi=0;tempi<procnum;tempi++)
	 			{
	 				for(int tempj=0;tempj<procnum;tempj++)
		 			{
	 					System.out.print(nb[tempi][tempj]);
		 			}
	 				System.out.println("");
	 			}
		
		
		//Coordinator is listening to processes

		ServerSocket ss = new ServerSocket(5056);
		int threads=0;
		Processtable pt = new Processtable();
		pt.putinfo(1, InetAddress.getByName(pid), 5056);
		while (threads<procnum-1) 
		{
			Socket s = null;
			
			try 
			{
				s = ss.accept();
				System.out.println("Register Message Received : " + s);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				
				//Coordinator assigns a Process ID to the register message

				Thread t = new Thread(new ClientHandler(s,dis,dos,pt,threads+2));
				t.start();
				threads++;
			}
			catch (Exception e){
				s.close();
				e.printStackTrace();
			}
		}
			TimeUnit.SECONDS.sleep(1);
			System.out.println("\n");
			NBInfo[] nblist = new NBInfo[procnum+1];
			
			//Creation of Neighbour Table
			
			for(int ii=1;ii<=procnum;ii++)
			{
				System.out.println(ii + " :  " + pt.getport(ii) + " : " + pt.gethostname(ii));
			}
			
			for(int ii=0;ii<procnum;ii++)
			{
				nblist[nb[ii][0]]=new NBInfo();
				for(int jj=1;jj<=procnum;jj++)
				{
					if(nb[ii][jj]!=0)
					{
						nblist[nb[ii][0]].putinfo(jj, nb[ii][jj], pt.gethostname(nb[ii][jj]), pt.getport(nb[ii][jj]));
					}
				}
			}
	
			//Output of the Neighbour Table
			for(int ii=1;ii<=procnum;ii++)
			{
				System.out.println(ii + " :-");
				for(int jo=1;jo<=nblist[ii].num;jo++)
				{
					System.out.println(nblist[ii].npid[jo] + " " + nblist[ii].neighbours[jo] + " " + nblist[ii].ports[jo]);
				}
			}
			
			//Send the Neighbour Table to all Processes
			for(int ii=2;ii<=procnum;ii++)
			{
				Socket serv = new Socket(pt.gethostname(ii),pt.getport(ii));
				ObjectOutputStream oos = new ObjectOutputStream(serv.getOutputStream());
				oos.flush();
				Thread t = new Thread(new ServerFirst(serv,ii,nblist,oos));
				t.start();					
			}
			int first=0;
			

			//Awaiting Ready Message
			for(int ii=1;ii<=nblist[1].num;ii++)
			{
				Socket sss = new Socket(nblist[1].getinetadd(ii),nblist[1].getport(ii));
				DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
				Thread th = new Thread(new ProcessComm(sss,dss,1,first,0,0));
			    th.start();
			}
			RetInt r = new RetInt();
			first++;
			int nbcount =0,retval=1;
			
			//Sending and Receiving First Compute Message
			while(true)
			{
				Socket servp = ss.accept();
				if(nbcount<nblist[1].num)
				{
				System.out.println("New Connection accepted : " + servp);	
				ProcessListener tt2 = new ProcessListener(servp,1);
				r=tt2.call();
				nbcount++;
				}
				else
				{
					Thread tt3 = new Thread(new ServerReady(servp,retval));
					tt3.start();
					retval++;
					if(retval==procnum)
					{
						System.out.println("\nReady Received From all");
						break;
					}
				}
			 }
			
						
			//Suzuki Kasami Begins
			
			int[] RN = new int[procnum+1];
			int seq_num=0;
			int flag=0;
			int[] temparr = new int[procnum+1];
			int tokenpresent=1,myowntoken=0;
			Token token = new Token();
			Token rtok = new Token();
			int totalmessages =0;
			int crisecnum =0 ;
			int completenum =0;
			Instant entertime;
			
			Instant requesttime=Instant.now();
			Long[] syncdelay = new Long[50];
			Long[] waittime = new Long[50];
			Long avgsync=0L;
			Long avgwait=0L;
			int syncwait=0;
			int avgmessages=0;
			
			Long[] totalsyncdelay = new Long[procnum-1];
			Long[] totalwaittime = new Long[procnum-1];
			Long totalavgsync=0L;
			Long totalavgwait=0L;
			int[] totalavgmessages= new int[procnum-1];
			int messageavg=0;
			int totalcounter=1;
			int totalcrisecnum=0;
			
			Writer wri = new FileWriter("localstate_1",true);
			PrintWriter pwri = new PrintWriter(wri,true);
			Writer wri2 = new FileWriter("Combined Results",true);
			PrintWriter pwri2 = new PrintWriter(wri2,true);
			
			while(true)
			{
				//Waiting for a message
				try
				{
				Socket servp = ss.accept();	
				ObjectInputStream os = new ObjectInputStream(servp.getInputStream());
					rtok = (Token)os.readObject();
					System.out.println("\n"+rtok.message);
					String[] inputbreak1 = rtok.message.split(" ");
					os.close();
					if(inputbreak1[0].equals("Request") && myowntoken!=1)
					{
					RN[Integer.parseInt(inputbreak1[2])] = Integer.parseInt(inputbreak1[3]);
					for(int ii=1;ii<=nblist[1].num+1;ii++)
					{
						System.out.print(RN[ii]+ " ");
					}
					System.out.println();
					//For the first iteration only, the coordinator initiates and sends token.
					}
					
					if(inputbreak1[0].equals("Completed"))
					{
						completenum++;
						totalsyncdelay[totalcounter] = rtok.avgsync;
						totalwaittime[totalcounter] = rtok.avgwait;
						totalavgmessages[totalcounter] = rtok.avgmessages;
						totalcounter++;
						totalcrisecnum += rtok.crisecnum; 
						
						if(completenum == procnum-2)
						{
							
							token.message = "Terminate";
							System.out.println("Terminate Sent");
							for(int ii=1;ii<=nblist[1].num;ii++)
							{
								Socket sss = new Socket(nblist[1].getinetadd(ii),nblist[1].getport(ii));
								ObjectOutputStream dss = new ObjectOutputStream(sss.getOutputStream());
								Thread th = new Thread(new TokenRequest(sss,dss,token));
								th.start();
							}
							
							for(int ij =0;ij<syncwait;ij++)
							{
								avgsync = avgsync + (syncdelay[ij]);
								avgwait = avgwait + waittime[ij];
							}
							
							avgsync = avgsync/crisecnum;
							avgwait = avgwait/crisecnum;
							avgmessages = (totalmessages/crisecnum);
							
							System.out.println();
							System.out.println("Number of Critical Section Entries = " + crisecnum);
							System.out.println("Number of Messages per CS entry = " + avgmessages);
							System.out.println("Average Sync Delay = " + avgsync);
							System.out.println("Average Wait Time = " + avgwait);
							System.out.println();
							pwri.println();
							pwri.println("Number of Critical Section Entries = " + crisecnum);
							pwri.println("Number of Messages per CS entry = " + avgmessages);
							pwri.println("Average Sync Delay = " + avgsync);
							pwri.println("Average Wait Time = " + avgwait);
							pwri.println();
							
							totalsyncdelay[0] = avgsync;
							totalwaittime[0] = avgwait;
							totalavgmessages[0] = avgmessages;
							
							for(int ij =0;ij<procnum-1;ij++)
							{
								totalavgsync = totalavgsync + totalsyncdelay[ij];
								totalavgwait = totalavgwait + totalwaittime[ij];
								messageavg = messageavg + totalavgmessages[ij];
							}
							
							totalavgsync =totalavgsync/totalcrisecnum;
							totalavgwait = totalavgwait/totalcrisecnum;
							messageavg = messageavg/totalcrisecnum;
							totalcrisecnum = totalcrisecnum+crisecnum;
							
							System.out.println();
							System.out.println("Total Number of Processes = " + procnum);
							System.out.println("Total Number of Critical Section Entries = " + totalcrisecnum);
							System.out.println("Total Number of Messages per CS entry = " + messageavg);
							System.out.println("Total Average Sync Delay = " + totalavgsync + "ms");
							System.out.println("Total Average Wait Time = " + totalavgwait + "ms");
							System.out.println();
							pwri.println();
							
							pwri2.println("Total Number of Processes = " + procnum);
							pwri2.println("Total Number of Critical Section Entries = " + totalcrisecnum);
							pwri2.println("Total Number of Messages per CS entry = " + messageavg);
							pwri2.println("Total Average Sync Delay = " + totalavgsync + "ms");
							pwri2.println("Total Average Wait Time = " + totalavgwait + "ms");
							pwri2.println();
							
							TimeUnit.SECONDS.sleep(4);
							System.out.println("Graceful Termination");
							System.exit(0);
						}
					}
					
					if(inputbreak1[0].equals("Token")||tokenpresent==1||myowntoken==1)
					{
						if(inputbreak1[0].equals("Token")||myowntoken==1)
						{
						entertime = Instant.now();
						System.out.println("Entered Critical Section\n");
						crisecnum++;
						token.leavetime = rtok.leavetime;
						
						Duration timeElapsed = Duration.between(token.leavetime,entertime);
						if(timeElapsed.toMillis()<0)
						{
							syncdelay[syncwait] = 1L;
						}
						else
							syncdelay[syncwait] = timeElapsed.toMillis();
						System.out.println("Sync Delay : "+ timeElapsed.toMillis() +" milliseconds");
						Duration time = Duration.between(requesttime,entertime);
						System.out.println("Wait Time : "+ time.toMillis() +" milliseconds");
						
						waittime[syncwait] = time.toMillis();
						syncwait++;
						
						pwri.println("Entered CS " + crisecnum);
						pwri.println("Syncronization Delay : " + timeElapsed.toMillis());
						pwri.println("Waiting Time : " + time.toMillis());
						pwri.println();
						
						TimeUnit.MILLISECONDS.sleep(t3);
						token.leavetime = rtok.leavetime;
						token.queue=rtok.queue;
						token.LN = rtok.LN;
						}
						
						System.out.println("\nRN is : ");
						for(int ii=1;ii<RN.length;ii++)
						{
							System.out.print(RN[ii]+ " ");
						}
						System.out.println("\nToken LN is : ");
						for(int ii=1;ii<RN.length;ii++)
						{
							System.out.print(token.LN[ii]+ " ");
						}
						
						//Subtraction
						for(int ii=0;ii<RN.length;ii++)
						{
							temparr[ii]=RN[ii]-token.LN[ii];
							token.LN[ii] = RN[ii];
						}
						
						//New Queue
						token.enqueue(temparr);
						System.out.println("\nQueue is");
						System.out.print("[");
						for(int ii=0;ii<temparr.length;ii++)
						{
							System.out.print(token.queue[ii]+ " ");
						}
						System.out.print("]");
						System.out.println();
						
						
						//Take the next process number in the queue
						int nextproc= token.dequeue();
						
						//Check if Queue is empty
						if(nextproc==0)
						{
							tokenpresent=1;
							continue;
						}
						
						//Check if Process 1 is the next in Queue 
						if(nextproc==1)
						{
							myowntoken=1;
							continue;
						}
						else
						{
							myowntoken=0;
						}
						
						//Send the Token to the next in Queue
						
						token.message="Token";
						token.leavetime = Instant.now();
						Socket ssse = new Socket(nblist[1].getinetadd(nextproc-1),nblist[1].getport(nextproc-1));
						ObjectOutputStream dsse = new ObjectOutputStream(ssse.getOutputStream());
						dsse.writeObject(token);
						dsse.flush();
						System.out.println("Token Sent to : " + nblist[1].npid[(nextproc-1)]);
						System.out.println();
						tokenpresent=0;
						flag=0;
					}
				
				
				if(flag==0)
				{
				Random gen2 = new Random();
				int rnd = gen2.nextInt(t2);
				if(rnd<t1)
					rnd+=t1;
				TimeUnit.MILLISECONDS.sleep(rnd);
				seq_num++;
				token.message = "Request from 1 " + seq_num;
				System.out.println("Request from 1 " + seq_num);
				for(int ii=1;ii<=nblist[1].num;ii++)
				{
					Socket sss = new Socket(nblist[1].getinetadd(ii),nblist[1].getport(ii));
					ObjectOutputStream dss = new ObjectOutputStream(sss.getOutputStream());
					Thread th = new Thread(new TokenRequest(sss,dss,token));
					th.start();
					
					flag=1;
					totalmessages++;
					
				}
				requesttime = Instant.now();
				RN[1]=seq_num;
				}
			}
			catch(IOException e1) {
		        e1.printStackTrace();
		    }
		}
	}
		
		
		//Client Half of the Program
				
		else
		{
			try
			{
				String pid=null;
				String str = System.getProperty("user.dir");
		 		str = str + "//dsConfig1";
				File file = new File(str);
		 		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		 		String line = null;
		 		int i=0; int t1=0,t2=0,t3=0;
		 		while( (line = br.readLine())!= null )
		 		{
		               		String [] tokens = line.split("\\s+");
		 			if(i==0)
		 			{
		 				pid=tokens[1];
		 			}	
		 			else if(i==1)
		 			{
		 				procnum=Integer.parseInt(tokens[3]);
		 			}
		 			else if(i==2)
		 			{
		 				t1=Integer.parseInt(tokens[1]);	
		 				t2=Integer.parseInt(tokens[2]);	
		 			}
		 			else if(i==3)
		 			{
		 				t3=Integer.parseInt(tokens[1]);
		 			}	
		 			i++;
		 			
		 		}
		 				 		
				//Register and Get ID from Server

				InetAddress ip = InetAddress.getByName(pid);
				Socket s = new Socket(ip, 5056);
				s.setReuseAddress(true);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				int myid = dis.readInt();
				System.out.println("The Process ID assigned is : " + myid);
				System.out.println(s.getLocalPort());
				s.close();
				NBInfo[] nbm = null;
				ServerSocket ss = new ServerSocket(s.getLocalPort());
				int tempi=0;
				int a=0;
				RetInt r = new RetInt();
				while(true)
				{
				Socket servs = null,servp=null;
				try 
				{
					
					if(tempi==0)
					{
						servs = ss.accept();
						ProcessHandler tt = new ProcessHandler(servs,myid);
						nbm = tt.call();
						for(int ii=1;ii<=nbm[myid].num;ii++)
						{
							Socket sss = new Socket(nbm[myid].getinetadd(ii),nbm[myid].getport(ii));
							DataOutputStream dss = new DataOutputStream(sss.getOutputStream());
							Thread th = new Thread(new ProcessComm(sss,dss,myid,tempi,0,0));
							th.start();
						}
						tempi=1;
					}
				
					else
					{
						servp = ss.accept();
						System.out.println("New Connection accepted : " + servp);	
						ProcessListener tt2 = new ProcessListener(servp,myid);
						r=tt2.call();
						a++;
						if(a==nbm[myid].num)
						{
							Socket servtemp = new Socket(ip, 5056);
							DataOutputStream doss = new DataOutputStream(servtemp.getOutputStream());
							doss.writeUTF("Ready");
							break;
						}
					}
					
				 }
				
				 catch (Exception e)
				 {
					s.close();
					e.printStackTrace();
				 }						
			}
				
				
			//Suzuki_Kasami Client Begins
				
			int[] RN = new int[nbm[myid].num+2];
			int seq_num=0; int flag=0;
			int tokenpresent=0;
			int myowntoken=0,norequests=0;
			Token ctok = new Token();
			Token rtok = new Token();
			int totalmessages = 0;
			int crisecnum = 0;
			Random gen2 = new Random();
			Instant entertime;
			Instant requesttime = Instant.now();
			
			int maxtime = gen2.nextInt(40);
			if(maxtime<20)
				maxtime+=20;
			
			Long[] syncdelay = new Long[maxtime];
			Long[] waittime = new Long[maxtime];
			Long avgsync=0L;
			Long avgwait=0L;
			int syncwait=0;
			int avgmessages=0;
			String filename = "localstate_" + myid;
			Writer wri = new FileWriter(filename,true);
			PrintWriter pwri = new PrintWriter(wri,true);
			
			
			while(true)
			{
				try
				{
				if((flag==0) && (myid!=2) && (seq_num<maxtime))
				{	
					int rnd = gen2.nextInt(t2);
					if(rnd<t1)
						rnd+=t1;
					TimeUnit.MILLISECONDS.sleep(rnd);
					seq_num++;
					if(norequests!=1)
					{
					ctok.message = "Request from "+ myid + " " + seq_num;
					System.out.println("Request number " + seq_num + " sent ");
					flag=1;
					
					for(int ii=1;ii<=nbm[myid].num;ii++)
					{
						Socket sss = new Socket(nbm[myid].getinetadd(ii),nbm[myid].getport(ii));
						ObjectOutputStream dss = new ObjectOutputStream(sss.getOutputStream());
						Thread th = new Thread(new TokenRequest(sss,dss,ctok));
						th.start();
						totalmessages++;
					}
					}
					else
						myowntoken=1;
				requesttime = Instant.now();
				RN[myid]=seq_num;
				}
				
				
				Socket servp = ss.accept();	
				ObjectInputStream dso = new ObjectInputStream(servp.getInputStream());
				rtok = (Token)dso.readObject();
				System.out.println(rtok.message);
				dso.close();
				String[] inputbreak1 = rtok.message.split(" ");
			
				if(inputbreak1[0].equals("Terminate"))
				{
					System.out.println("Graceful Termination");
					System.exit(0);
				}
				
				if(inputbreak1[0].equals("Request"))
				{
					RN[Integer.parseInt(inputbreak1[2])] = Integer.parseInt(inputbreak1[3]);
					for(int ii=1;ii<=nbm[myid].num+1;ii++)
					{
						System.out.print(RN[ii]+ " ");
					}
					System.out.println();
				}
				
				if(inputbreak1[0].equals("Token")||myowntoken==1||norequests==1)
				{
					if(inputbreak1[0].equals("Token")||myowntoken==1)
					{
					System.out.println("\nEntered Critical Section\n");
					
					entertime = Instant.now();
					ctok.leavetime = rtok.leavetime;
					Duration timeElapsed = Duration.between(ctok.leavetime,entertime);
					if(timeElapsed.toMillis()<0)
					{
						syncdelay[syncwait] = 1L;
					}
					else
					{
						syncdelay[syncwait] = timeElapsed.toMillis();
					}
					System.out.println("Sync Delay : "+ timeElapsed.toMillis() +" milliseconds");
					Duration time = Duration.between(requesttime,entertime);
					System.out.println("Wait Time : "+ time.toMillis() +" milliseconds");
					
					pwri.println("Entered CS " + crisecnum);
					pwri.println("Syncronization Delay : " + timeElapsed.toMillis());
					pwri.println("Waiting Time : " + time.toMillis());
					pwri.println();
					
					
					waittime[syncwait] = time.toMillis();
					syncwait++;
					
					
					TimeUnit.MILLISECONDS.sleep(t3);
					crisecnum++;
					ctok.LN = rtok.LN;
					ctok.queue = rtok.queue;
					}
					System.out.println("\nRN is : ");
					for(int ii=1;ii<RN.length;ii++)
					{
						System.out.print(RN[ii] + " ");
					}
					int[] temparr = new int[RN.length];
					System.out.println("\nToken LN is : ");
					for(int ii=1;ii<RN.length;ii++)
					{
						System.out.print(ctok.LN[ii] + " ");
					}
					
					//Subtraction
					for(int ii=0;ii<RN.length;ii++)
					{
						temparr[ii]=RN[ii]-ctok.LN[ii];
						ctok.LN[ii] = RN[ii];
					}
					
					ctok.enqueue(temparr);
					System.out.println("\nToken Queue is");
					System.out.print("[");
					for(int ii=0;ii<temparr.length;ii++)
					{
						System.out.print(ctok.queue[ii] + " ");
					}
					System.out.print("]");
					System.out.println();
					
					int nextproc = ctok.dequeue();
					ctok.message = "Token";
					
					if(nextproc==0)
					{
						norequests=1;
						continue;
					}
					else
						norequests=0;
					if(nextproc==myid)
					{
						myowntoken=1;
						continue;
					}
					else
						myowntoken=0;
					
					int xyz;
					for(xyz = 0;xyz<nbm[myid].num;xyz++)
					{
						if(nbm[myid].npid[xyz]==nextproc)
							break;
					}
					
					ctok.leavetime = Instant.now();
					Socket ssse = new Socket(nbm[myid].getinetadd(xyz),nbm[myid].getport(xyz));
					ObjectOutputStream dsse = new ObjectOutputStream(ssse.getOutputStream());
					dsse.writeObject(ctok);
					System.out.println("Token Sent to : " + nbm[myid].npid[xyz] + "\n\n");
					dsse.flush();
					flag=0;
				}
				
				if(crisecnum>=maxtime)
				{
					ctok.message = "Completed";
					System.out.println("Completed Message Sent to Coordinator");
					
					for(int ij =0;ij<syncwait;ij++)
					{
						avgsync = avgsync + (syncdelay[ij]);
						avgwait = avgwait+waittime[ij];
					}
					
					avgsync = avgsync/crisecnum;
					avgwait = avgwait/crisecnum;
					avgmessages = (totalmessages/crisecnum);
					System.out.println();
					System.out.println("Number of Critical Section Entries = " + crisecnum);
					System.out.println("Number of Messages per CS entry = " + avgmessages);
					System.out.println("Average Sync Delay = " + avgsync+ "ms");
					System.out.println("Average Wait Time = " + avgwait+ "ms");
					System.out.println();
					pwri.println();
					pwri.println("Number of Critical Section Entries = " + crisecnum);
					pwri.println("Number of Messages per CS entry = " + avgmessages);
					pwri.println("Average Sync Delay = " + avgsync + "ms");
					pwri.println("Average Wait Time = " + avgwait + "ms");
					pwri.println();
					
					ctok.avgmessages = avgmessages*crisecnum;
					ctok.avgsync = avgsync*crisecnum;
					ctok.avgwait = avgwait*crisecnum;
					ctok.crisecnum = crisecnum;
					Socket sss = new Socket(nbm[myid].getinetadd(1),nbm[myid].getport(1));
					ObjectOutputStream dss = new ObjectOutputStream(sss.getOutputStream());
					dss.writeObject(ctok);
					dss.flush();
					break;
				}
			}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
				
			
			while(true)
			{
				try
				{
				Socket servp = ss.accept();	
				ObjectInputStream dso = new ObjectInputStream(servp.getInputStream());
				rtok = (Token)dso.readObject();
				System.out.println(rtok.message);
				dso.close();
				String[] inputbreak1 = rtok.message.split(" ");
				if(inputbreak1[0].equals("Terminate"))
				{
					System.out.println("");
					System.out.println("Graceful Termination");
					System.out.println("");
					System.exit(0);
				}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
			
	}
}
}


class ClientHandler implements Runnable
{
	final DataInputStream dis;
	final DataOutputStream dos;
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	
	public ClientHandler(Socket s,DataInputStream dis, DataOutputStream dos,Processtable ppt,int pid) 
	{
		this.s = s;
		this.pt = ppt;
		this.pid=pid;
		this.dis = dis;
		this.dos=dos;
	}
	
	public void  run() 
	{
		
		try {
				dos.writeInt(pid);
				//int port = dis.readInt();
				InetAddress addr = s.getInetAddress();
				int port = s.getPort();
				System.out.print(" Addr : " + addr + " port : " + port + "\n\n");
				pt.putinfo(pid, addr, port);						
			} catch (IOException e) {
				e.printStackTrace();
			}
				
		try
		{
			// closing resources
			dis.close();
			dos.close();
			
			
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
}

class NBInfo implements Serializable {
	int[] npid = new int[30];
	int num;
	InetAddress[] neighbours = new InetAddress[30];
	int[] ports= new int[30];
	public void putinfo(int num, int ppid, InetAddress nb, int pport)
	{
		this.num=num;
		this.npid[num]=ppid;
		this.neighbours[num]=nb;
		this.ports[num]=pport;
		
	}
	public InetAddress getinetadd(int num)
	{		
		return neighbours[num];
	}
	public int getport(int num)
	{		
		return ports[num];
	}
}


class ProcessComm implements Runnable{
	Socket sss;
	DataOutputStream dss;
	int a=0,pid,first=0,lclock=0,fmc=0;  
	public ProcessComm(Socket sss, DataOutputStream dss, int myid,int num,int LCLOCK,int finalmarkercount)
	{
		this.dss = dss;
		this.sss = sss;
		this.pid = myid;
		this.first = num;
		this.lclock = LCLOCK;
		this.fmc = finalmarkercount;
	}
	
	public void  run() 
	{
		
		try {
				if(first==0)
				{
					dss.writeUTF("Hello from " + pid);
				//	dss.flush();
				
				}
				if(first==-1)
				{
					dss.writeUTF("Marker " + fmc);
				}
				if(first!=0)
				{
					dss.writeUTF("Compute from " + pid + " " + lclock);
				//	dss.flush();
					
				}
				
								
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}


}

class ProcessHandler implements Callable{
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	ObjectInputStream ois;
	NBInfo[] nb;
	public ProcessHandler(Socket s,int myid) 
	{
		this.s = s;
		this.pid = myid;
	}
	
	public NBInfo[] call() throws Exception
	{
		
		try {
				ObjectInputStream ooois = new ObjectInputStream(s.getInputStream());
				nb = (NBInfo[])ooois.readObject();
						ooois.close();		
			} catch (Exception e) {
				e.printStackTrace();
			}
		return nb;	
	}
}


class ProcessListener implements Callable {
	final Socket s;
	int a=0,pid;  
	Processtable pt = new Processtable();
	RetInt r = new RetInt();
	public ProcessListener(Socket ss,int myid) 
	{
		this.s = ss;
		this.pid = myid;
	}
	
	public RetInt call() throws Exception
	{
		try {
				DataInputStream ds = new DataInputStream(s.getInputStream());
				String input = ds.readUTF();
				String[] input2 = input.split(" ",4);
				String[] input3 = input.split(" ", 2);
				//Incase the input message is Compute
				if(input2[0].equals("Compute"))
				{
					System.out.println("\n" + input);
					r.val=Integer.parseInt(input2[2]);
					r.clock = Integer.parseInt(input2[3]);
					return r;
				}
				
				//Incase the message is Marker
				else if(input3[0].equals("Marker"))
				{
					r.val = -1;
					r.clock = Integer.parseInt(input3[1]);
					return r;
				}
				
				//Incase the Input Message is Ready
				else
				{
					System.out.println("\n"+ input);
					
				}
				
				
				
						
			} catch (Exception e) {
				e.printStackTrace();
			}
		r.val=0;
		return r;
					
	}

}

class Processtable {
	int[] pid = new int[100];
	int[] port = new int[100];
	InetAddress hname[] = new InetAddress[100];
	
	public void putinfo(int ppid, InetAddress hhname, int pport)
	{
		
		hname[ppid] = hhname;
		port[ppid]=pport;
			
	}
	public int getport(int ppid)
	{
		return port[ppid];
	}
	public InetAddress gethostname(int ppid)
	{
		return hname[ppid];
	}
}

class RetInt {
	int val;
	int clock;
}

class ServerFirst implements Runnable{
	final Socket s;
	int a=0,pid;	
	NBInfo[] nblist = new NBInfo[10];
	ObjectOutputStream oos;
	public ServerFirst(Socket s, int myid, NBInfo[] nblist,ObjectOutputStream oos) 
	{
		this.s = s;
		this.pid = myid;
		this.oos = oos;
		this.nblist = nblist;
	}
	
	public void  run() 
	{
		
		try {
				oos.writeObject(nblist);
				oos.flush();
				oos.close();
				System.out.println("\nObject Sent to Process");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}
}

class ServerReady implements Runnable{
	final Socket s;
	int processnum;
	public ServerReady(Socket s,int processnum) 
	{
		this.s = s;
		this.processnum = processnum;
	}
	
	public void  run() 
	{
		
		try {
				System.out.println("Ready Received from Process : " + processnum);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
					
	}
}


