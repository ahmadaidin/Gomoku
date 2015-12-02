public class Point{

  private int x;
  private int y;

  public Point(){
    x=y=0;
  }

  public Point(int _x, int _y){
    x=_x;
    y=_y;
  }

  public int getAbsis(){
    return x;
  }

  public int getOrdinat(){
    return y;
  }

  public void setAbis(int _x){
    x=_x;
  }

}
