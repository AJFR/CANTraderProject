import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

class MockOM extends Thread{
    InetSocketAddress[] clients;
    InetSocketAddress[] routers;
    InetSocketAddress trader;
    LiveMarketData liveMarketData;
    MockOM(String name,InetSocketAddress[] routers,InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData){
        this.clients=clients;
        this.routers=routers;
        this.trader=trader;
        this.liveMarketData=liveMarketData;
        this.setName(name);
    }
    @Override
    public void run(){
        try{
            //In order to debug constructors you can do F5 F7 F5
            new OrderManager(routers,clients,trader,liveMarketData);
        }catch(IOException | ClassNotFoundException | InterruptedException ex){
            Logger.getLogger(MockOM.class.getName()).log(Level.SEVERE,null,ex);
        }
    }

}