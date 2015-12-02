public class Client{
  private String addr;
  private int port;
  private boolean turn;

  public Client(){
    addr=""
    port=0;
    turn = false;
  }

  public Client(String _addr, _port, _turn){
    addr=_addr;
    port=_port;
    turn= turn;
  }

  public void setAddr(String _addr){
    addr=_addr;
  }

  public void setPort(int _port){
    port=_port;
  }

  public void setTurn(boolean _turn){
    turn=_turn;
  }

  public String getAddr(){
    return addr;
  }

  public int getPort(){
    return port;
  }

  public int getTurn(){
    return turn;
  }

  

}
