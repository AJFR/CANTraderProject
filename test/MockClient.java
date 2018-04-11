import java.io.IOException;

class MockClient extends Thread{

    int port;

    MockClient(String name,int port){
        this.port=port;
        this.setName(name);
    }

    public void run(){
        try {
            SampleClient client = new SampleClient(port);
            if( port == 2000 ){
                //TODO why does this take an arg?
                client.sendOrder(null);
                //int id=client.sendOrder(null);
                //TODO client.sendCancel(id);
                client.messageHandler();
            }else{
                client.sendOrder(null);
                client.messageHandler();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}