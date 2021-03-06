import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import javax.net.ServerSocketFactory;

import OrderManager.Order;
import TradeScreen.TradeScreen;

public class Trader extends Thread implements TradeScreen {

    private HashMap<Integer, Order> orders = new HashMap<>();
    private static Socket orderManagerConnection;
    private int port;


    private int countOrders = 0;
    private boolean ordersAreOpen = true;

    Trader(String name, int port) {
        this.setName(name);
        this.port = port;
    }

    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;

    private void checkOrdersOpen(){
        if(countOrders == orders.size()) {
            ordersAreOpen = false;
            System.out.println("No more orders. Trader is shutting down.");
        }
    }


    public void run() {
        //OM will connect to us
        try {
            orderManagerConnection = ServerSocketFactory.getDefault().createServerSocket(port).accept();

            //objectInputStream=new ObjectInputStream( orderManagerConnection.getInputStream());
            InputStream s = orderManagerConnection.getInputStream(); //if i try to create an objectinputstream before we have data it will block
            while (ordersAreOpen) {
                if (0 < s.available()) {
                    objectInputStream = new ObjectInputStream(s);  //TODO check if we need to create each time. this will block if no data, but maybe we can still try to create it once instead of repeatedly
                    orderRequest method = (orderRequest) objectInputStream.readObject();
                    System.out.println(Thread.currentThread().getName() + " calling: " + method);
                    switch (method) {
                        case newOrder:
                            newOrder(objectInputStream.readInt(), (Order) objectInputStream.readObject());
                            break;
                        case price:
                            price(objectInputStream.readInt(), (Order) objectInputStream.readObject());
                            break;
                        case cross:
                            objectInputStream.readInt();
                            objectInputStream.readObject();
                            break; //TODO
                        case fill:
                            fill(objectInputStream.readInt(),(Order)objectInputStream.readObject());
                            //objectInputStream.readInt();
                            //objectInputStream.readObject();
                            break; //TODO
                    }
                } else {
                    System.out.println("Trader Waiting for data to be available - sleep 1s");
                    Thread.sleep(1000);
                }
                if (orders.size() > 0){
                    checkOrdersOpen();
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void newOrder(int id, Order order) throws IOException, InterruptedException {
        //TODO the order should go in a visual grid, but not needed for test purposes
        Thread.sleep(2134);
        orders.put(id, order);
        System.out.println("newOrder placed, order id: "+id+" ");
        acceptOrder(id);
    }

    @Override
    public void acceptOrder(int id) throws IOException {
        objectOutputStream = new ObjectOutputStream(orderManagerConnection.getOutputStream());
        objectOutputStream.writeObject("acceptOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.flush();
    }

    @Override
    public void sliceOrder(int id, int sliceSize) throws IOException {
        objectOutputStream = new ObjectOutputStream(orderManagerConnection.getOutputStream());
        objectOutputStream.writeObject("sliceOrder");
        objectOutputStream.writeInt(id);
        objectOutputStream.writeInt(sliceSize);
        objectOutputStream.flush();
    }

    @Override
    public synchronized void price(int id, Order o) throws InterruptedException, IOException {
        //TODO should update the trade screen
//        Thread.sleep(2134);
        //System.out.println("(Trader.price)Order ID: "+id+" is being sliced in price");
        int sliceSize = o.getInitialOrderSize()/10;
        if(sliceSize> o.sizeRemaining()) {
            sliceSize = o.sizeRemaining();
        }
        sliceOrder(id, sliceSize);
//        sliceOrder(id, orders.get(id).sizeRemaining()/4);
    }

    public void fill(int id, Order o) throws InterruptedException, IOException{
        System.out.println(o.toString());
        System.out.println("---------------");
        System.out.println("order id: "+id);
        System.out.println("o.sizeRemaining: "+o.sizeRemaining());
        System.out.println("o.sliceSizes: "+o.sliceSizes());
        System.out.println("o.sizeFilled: "+o.sizeFilled());
        System.out.println("---------------");
        if(o.sizeRemaining() > 0){
//            System.out.println("Size Remaining: "+o.sizeRemaining()+" Size Filled: "+o.sizeFilled()+" Slice Size: "+o.sliceSizes());
            price(id,o);
//            sliceOrder(id,orders.get(id).sizeRemaining()); //the order of this may provide problems, move to bottom of method?
            objectOutputStream = new ObjectOutputStream(orderManagerConnection.getOutputStream());
            objectOutputStream.writeObject("partiallyFilledOrder");
            objectOutputStream.writeObject(o);
            objectOutputStream.flush();
        }else{
            objectOutputStream = new ObjectOutputStream(orderManagerConnection.getOutputStream());
            objectOutputStream.writeObject("filledOrder");
            objectOutputStream.writeObject(o);
            objectOutputStream.flush();
            countOrders++;

        }
    }
}