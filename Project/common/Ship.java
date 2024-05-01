package Project.common;

import java.io.Serializable;

public class Ship implements Serializable{
  private int anchorX;
  private int anchorY;
  private String orientation;
  // private int[] fuselage; // TODO: could use this and procedureally draw board each time from just list of ships 
  private ShipType type;

  public Ship(ShipType type) { 
    this.type = type;
    // fuselage = new int[type.getLength()];
  }

  public Ship(int anchorX, int anchorY, String orientation, ShipType type) {
    this.anchorX = anchorX;
    this.anchorY = anchorY;
    this.orientation = orientation;
    this.type = type;
    // fuselage = new int[type.getLength()];
  }

  public Ship(Ship ship) {
    this.anchorX = ship.getAnchorX();
    this.anchorY = ship.getAnchorY();
    this.orientation = ship.getOrientation();
    this.type = ship.getType();
    // this.fuselage = ship.getFuselage();
  }

  public void setAnchorX(int anchorX) { this.anchorX = anchorX; }

  public int getAnchorX() { return anchorX; }

  public void setAnchorY(int anchorY) { this.anchorY = anchorY; }

  public int getAnchorY() { return anchorY; }

  public void setOrientation(String orientation) { this.orientation = orientation; }

  public String getOrientation() { return orientation; }

  public void setType(ShipType type) { this.type = type; }

  public ShipType getType() { return type; }

  // public void setFuselage(int[] fuselage) { this.fuselage = fuselage; }

  // public int[] getFuselage() { return fuselage; }

  // public void setFuselage(int index, int value) { fuselage[index] = value; }

  // public int getFuselage(int index) { return fuselage[index]; }

}