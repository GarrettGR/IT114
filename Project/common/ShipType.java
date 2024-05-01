package Project.common;

public enum ShipType {
  CARRIER(5, "Carrier"),
  BATTLESHIP(4, "Battleship"),
  CRUISER(3, "Cruiser"),
  SUBMARINE(3, "Submarine"),
  DESTROYER(2, "Destroyer"),
  LIFE_BOAT(1, "Life Boat");

  private final int length;
  private final String name;

  ShipType(int length, String name) {
    this.length = length;
    this.name = name;
  }

  public int getLength() { return length; }

  public String getName() { return name; }
}