import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	private static final Random RANDOM_NUM_GENERATOR=new Random();
	private static final Instrument[] INSTRUMENTS={new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
	private static final HashMap OUT_QUEUE=new HashMap(); //queue for outgoing orders
	private int id=0; //message id number
	private Socket omConn; //connection to order manager
			
	public SampleClient(int port) throws IOException{
		//OM will connect to us
		omConn=new ServerSocket(port).accept();
		System.out.println("OM connected to client port "+port);
	}
	
	@Override
	public int sendOrder(Object par0)throws IOException{
		int size=RANDOM_NUM_GENERATOR.nextInt(5000);
		int instid=RANDOM_NUM_GENERATOR.nextInt(3);
		Instrument instrument=INSTRUMENTS[RANDOM_NUM_GENERATOR.nextInt(INSTRUMENTS.length)];
		NewOrderSingle nos=new NewOrderSingle(size,instid,instrument);
		
		show("sendOrder: id="+id+" size="+size+" instrument="+INSTRUMENTS[instid].toString());
		OUT_QUEUE.put(id,nos);
		if(omConn.isConnected()){
			ObjectOutputStream os=new ObjectOutputStream(omConn.getOutputStream());
			os.writeObject("newOrderSingle");
			//os.writeObject("35=D;");
			os.writeInt(id);
			os.writeObject(nos);
			os.flush();
		}
		return id++;
	}

	@Override
	public void sendCancel(int idToCancel){
		show("sendCancel: id="+idToCancel);
		if(omConn.isConnected()){
			//OMconnection.sendMessage("cancel",idToCancel);
		}
	}

	@Override
	public void partialFill(Order order){show(""+order);
	}

	@Override
	public void fullyFilled(Order order){show(""+order);
		OUT_QUEUE.remove(order.ClientOrderID);
	}

	@Override
	public void cancelled(Order order){show(""+order);
		OUT_QUEUE.remove(order.ClientOrderID);
	}

	enum methods{newOrderSingleAcknowledgement,dontKnow};
	@Override
	public void messageHandler(){
		
		ObjectInputStream is;
		try {
			while(true){
				//is.wait(); //this throws an exception!!
				while(0<omConn.getInputStream().available()){
					is = new ObjectInputStream(omConn.getInputStream());
					String fix=(String)is.readObject();
					System.out.println(Thread.currentThread().getName()+" received fix message: "+fix);
					String[] fixTags=fix.split(";");
					int OrderId=-1;
					char MsgType;
					int OrdStatus;
					methods whatToDo=methods.dontKnow;
					//String[][] fixTagsValues=new String[fixTags.length][2];
					for(int i=0;i<fixTags.length;i++){
						String[] tag_value=fixTags[i].split("=");
						switch(tag_value[0]){
							case"11":OrderId=Integer.parseInt(tag_value[1]);break;
							case"35":MsgType=tag_value[1].charAt(0);
								if(MsgType=='A')whatToDo=methods.newOrderSingleAcknowledgement;
								break;
							case"39":OrdStatus=tag_value[1].charAt(0);break;
						}
					}
					switch(whatToDo){
						case newOrderSingleAcknowledgement:newOrderSingleAcknowledgement(OrderId);
					}
					
					/*message=connection.getMessage();
					char type;
					switch(type){
						case 'C':cancelled(message);break;
						case 'P':partialFill(message);break;
						case 'F':fullyFilled(message);
					}*/
					show("");
				}
			}
		} catch (IOException|ClassNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void newOrderSingleAcknowledgement(int OrderId){
		System.out.println(Thread.currentThread().getName()+" called newOrderSingleAcknowledgement");
		//do nothing, as not recording so much state in the NOS class at present
	}
/*listen for connections
once order manager has connected, then send and cancel orders randomly
listen for messages from order manager and print them to stdout.*/
}