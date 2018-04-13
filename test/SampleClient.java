import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

import OrderClient.Client;
import OrderClient.NewOrderSingle;
import OrderManager.Order;
import Ref.Instrument;
import Ref.Ric;

public class SampleClient extends Mock implements Client{

	enum methods{newOrderSingleAcknowledgement,dontKnow}

	private static final Random RANDOM_NUM_GENERATOR=new Random();
	private static final Instrument[] INSTRUMENTS = {
			new Instrument(new Ric("VOD.L")),
			new Instrument(new Ric("BP.L")),
			new Instrument(new Ric("BT.L"))
	};
	private static final HashMap OUT_QUEUE=new HashMap(); //queue for outgoing orders
	// CASSY to rename to messageID
	private int messageID = 0; //message id number

	private Socket OrderManagerConnection; //connection to order manager

	public SampleClient(int port) throws IOException{
		//Order Manager connecting to client port
		//CASSY: omConn renamed to OrderManagerConnection
		//OM will connect to us
		OrderManagerConnection =new ServerSocket(port).accept();
		System.out.println("OM connected to client port "+port);
	}

	@Override
	public int sendOrder(Object par0) throws IOException{
		//defining instrument with a randomNoGenerator
		int size=RANDOM_NUM_GENERATOR.nextInt(5000);
		// CASSY: renamed instid to instrumentID
		int instrumentID = RANDOM_NUM_GENERATOR.nextInt(3);
		Instrument instrument = INSTRUMENTS[RANDOM_NUM_GENERATOR.nextInt(INSTRUMENTS.length)];
		NewOrderSingle nos = new NewOrderSingle(size,instrumentID,instrument);

		show(" sendOrder: messageID="+messageID+" size="+size+" instrument="+INSTRUMENTS[instrumentID].toString());
		OUT_QUEUE.put(messageID,nos);

		if(OrderManagerConnection.isConnected()){
			ObjectOutputStream os=new ObjectOutputStream(OrderManagerConnection.getOutputStream());
			os.writeObject("newOrderSingle");
//			try {
//				ObjectInputStream objectInputStream = new ObjectInputStream(OrderManagerConnection.getInputStream());
//			}
//			catch(StreamCorruptedException e){
//				System.out.println("InputStream IOException");
//			}
			//System.out.println("stream availability "+objectInputStream.available());
			//os.writeObject("35=D;");
			os.writeInt(messageID);
			os.writeObject(nos);
//			System.out.println("Read in from inputstream "+os.getClass().getName().toString());

			os.flush();
		}
		return messageID++;
	}

	@Override
	public void sendCancel(int idToCancel){
		show("sendCancel: id="+idToCancel);
		if(OrderManagerConnection.isConnected()){
			//OMconnection.sendMessage("cancel",idToCancel);
		}
	}

	@Override
	public void partialFill(Order order){
		show(" partially filled order: "+order);
	}

	@Override
	public void fullyFilled(Order order){
		show(" fully filled order: "+order);
		OUT_QUEUE.remove(order.clientOrderID);
	}

	@Override
	public void cancelled(Order order){show(" cancelled order: "+order);
		OUT_QUEUE.remove(order.clientOrderID);
	}

	@Override
	public void messageHandler(){
		
		ObjectInputStream inputStream;
		try {
			while(true){			// this while(true) highly unnecessary; replace with something
				//inputStream.wait(); //this throws an exception!!
//				System.out.println("While loop value "+OrderManagerConnection.getInputStream().available());
				while( 0 < OrderManagerConnection.getInputStream().available() )
				{
					inputStream = new ObjectInputStream(OrderManagerConnection.getInputStream());
					String fix = (String)inputStream.readObject();
					System.out.println(Thread.currentThread().getName()+" received fix message: "+fix);
					String[] fixTags = fix.split(";");
					int OrderId = -1;
					char MsgType;
					int OrdStatus;
					methods whatToDo = methods.dontKnow;
					//String[][] fixTagsValues=new String[fixTags.length][2];

						for( int i=0 ; i < fixTags.length ; i++ )
						{
							String[] tag_value = fixTags[i].split("=");
							switch(tag_value[0]){
								case"11":
									OrderId = Integer.parseInt(tag_value[1]);
									break;
								case"35":
									MsgType = tag_value[1].charAt(0);
									if(MsgType=='A')
										whatToDo = methods.newOrderSingleAcknowledgement;
									break;
								case"39":
									OrdStatus = tag_value[1].charAt(0);
									//Order message=(Order) inputStream.readObject();
									switch(OrdStatus){
										case '4':cancelled(null);break;
										case '1':partialFill(null);break;
										case '2':fullyFilled(null);
									}

									break;
							}
						}
						switch(whatToDo){
							case newOrderSingleAcknowledgement:
								newOrderSingleAcknowledgement(OrderId);
						}

//					show("");		// this outputs the line: Client 1:
				}
			}
		} catch (IOException|ClassNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void newOrderSingleAcknowledgement(int OrderId){
		System.out.println(Thread.currentThread().getName()+" called: newOrderSingleAcknowledgement");
		//do nothing, as not recording so much state in the NOS class at present
	}

	/*
	listen for connections once order manager has connected, then send and cancel orders randomly
	listen for messages from order manager and print them to stdout.
	*/

}